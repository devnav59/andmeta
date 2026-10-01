package com.example.repository

import com.example.model.ClosePositionCommand
import com.example.model.ConnectionStatus
import com.example.model.ModifySlTpCommand
import com.example.model.OpenOrderCommand
import com.example.model.OrderType
import com.example.model.Position
import com.example.model.TradingStats
import com.example.network.MetaTraderWebSocketClient
import com.example.network.MultiProtocolBridgeServer
import com.example.network.NetworkUtils
import com.example.network.SimulatorBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

object TradingRepository {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val wsClient = MetaTraderWebSocketClient()
    private val simulator = SimulatorBridge()
    private val multiBridgeServer = MultiProtocolBridgeServer()

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isServerListening = MutableStateFlow(true)
    private val _lastDataReceivedTimestamp = MutableStateFlow(0L)
    private val _directPositions = MutableStateFlow<List<Position>>(emptyList())

    private val queuedCommands = Collections.synchronizedList(mutableListOf<JSONObject>())

    private val _logs = MutableStateFlow<List<String>>(listOf("System initialized. Multi-Protocol Server ready."))
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        _isSimulationMode,
        wsClient.status,
        _lastDataReceivedTimestamp,
        _isServerListening
    ) { isSim, wsStatus, lastReceived, isListening ->
        val now = System.currentTimeMillis()
        val isRecentlyActive = (now - lastReceived) < 4000L

        when {
            isSim -> ConnectionStatus.SIMULATED
            isRecentlyActive -> ConnectionStatus.CONNECTED
            wsStatus == ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
            isListening -> ConnectionStatus.LISTENING
            wsStatus == ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
            wsStatus == ConnectionStatus.ERROR -> ConnectionStatus.ERROR
            else -> ConnectionStatus.DISCONNECTED
        }
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionStatus.LISTENING)

    val positions: StateFlow<List<Position>> = combine(
        _isSimulationMode,
        wsClient.positions,
        simulator.positions,
        _directPositions
    ) { isSim, wsPos, simPos, directPos ->
        when {
            isSim -> simPos
            directPos.isNotEmpty() -> directPos
            else -> wsPos
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<TradingStats> = positions.combine(_isSimulationMode) { list, _ ->
        val totalProfit = list.sumOf { it.profit }
        val totalLots = list.sumOf { it.volume }
        val buyCount = list.count { it.isBuy }
        val sellCount = list.count { it.isSell }
        TradingStats(
            totalProfit = (totalProfit * 100).toInt() / 100.0,
            totalLots = (totalLots * 100).toInt() / 100.0,
            openCount = list.size,
            buyCount = buyCount,
            sellCount = sellCount
        )
    }.stateIn(scope, SharingStarted.Eagerly, TradingStats())

    init {
        scope.launch {
            wsClient.events.collect { appendLog(it) }
        }
        scope.launch {
            simulator.events.collect { appendLog("[SIM] $it") }
        }

        // Start multi-protocol bridge server on 0.0.0.0:8080
        multiBridgeServer.start(_serverPort.value)
        _isServerListening.value = true

        // Heartbeat monitor for live connection status
        scope.launch {
            while (isActive) {
                delay(2000)
                val now = System.currentTimeMillis()
                if (now - _lastDataReceivedTimestamp.value > 5000L && _directPositions.value.isNotEmpty()) {
                    // Mark inactive if no ping from EA for 5s
                    _lastDataReceivedTimestamp.value = 0L
                }
            }
        }
    }

    fun setServerConfig(ip: String, port: Int) {
        _serverIp.value = ip
        _serverPort.value = port
        multiBridgeServer.start(port)
        _isServerListening.value = true
        appendLog("Server port reconfigured to $port")
    }

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun connect() {
        if (_isSimulationMode.value) {
            simulator.start()
            appendLog("Connected to local simulation engine")
            return
        }

        // Start local server to receive Winlator / MT connection
        multiBridgeServer.start(_serverPort.value)
        _isServerListening.value = true
        appendLog("Server is actively listening on port ${_serverPort.value} (Ready for Winlator)")

        // If IP is not 127.0.0.1, also initiate outgoing WebSocket connection
        if (_serverIp.value != "127.0.0.1" && _serverIp.value != "localhost") {
            wsClient.connect(_serverIp.value, _serverPort.value)
        }
    }

    fun disconnect() {
        if (_isSimulationMode.value) {
            simulator.stop()
            appendLog("Simulation engine stopped")
        } else {
            wsClient.disconnect()
            _lastDataReceivedTimestamp.value = 0L
            appendLog("Client disconnected")
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (enabled) {
            wsClient.disconnect()
            _lastDataReceivedTimestamp.value = 0L
            simulator.start()
            appendLog("Switched to Simulator Mode (Test MetaTrader EA without Winlator/PC)")
        } else {
            simulator.stop()
            appendLog("Exited Simulator Mode. Ready for live Winlator/MT connection.")
        }
    }

    fun updatePositionsFromHttp(list: List<Position>) {
        _directPositions.value = list
        _lastDataReceivedTimestamp.value = System.currentTimeMillis()
    }

    fun enqueueCommand(json: JSONObject) {
        queuedCommands.add(json)
        appendLog("Queued command: ${json.optString("action")}")
    }

    fun drainPendingCommandsJson(): String {
        val array = JSONArray()
        synchronized(queuedCommands) {
            for (cmd in queuedCommands) {
                array.put(cmd)
            }
            queuedCommands.clear()
        }
        return array.toString()
    }

    fun openOrder(
        symbol: String,
        type: OrderType,
        volume: Double,
        price: Double = 0.0,
        slPips: Double = 0.0,
        tpPips: Double = 0.0
    ): Boolean {
        val cmd = OpenOrderCommand(
            symbol = symbol.uppercase().trim(),
            type = type.name,
            volume = volume,
            price = price,
            sl_pips = slPips,
            tp_pips = tpPips
        )

        val json = JSONObject().apply {
            put("action", "OPEN_ORDER")
            put("symbol", cmd.symbol)
            put("type", cmd.type)
            put("volume", cmd.volume)
            put("price", cmd.price)
            put("sl_pips", cmd.sl_pips)
            put("tp_pips", cmd.tp_pips)
        }
        queuedCommands.add(json)
        appendLog("Sent OPEN_ORDER: ${cmd.type} ${cmd.volume} ${cmd.symbol}")

        if (_isSimulationMode.value) {
            simulator.handleOpenOrder(cmd)
            return true
        }

        wsClient.sendOpenOrder(cmd)
        return true
    }

    fun modifySlTp(ticket: Long, slDelta: Double, tpDelta: Double): Boolean {
        val cmd = ModifySlTpCommand(
            ticket = ticket,
            sl_pips_delta = slDelta,
            tp_pips_delta = tpDelta
        )

        val json = JSONObject().apply {
            put("action", "MODIFY_SL_TP")
            put("ticket", cmd.ticket)
            put("sl_pips_delta", cmd.sl_pips_delta)
            put("tp_pips_delta", cmd.tp_pips_delta)
        }
        queuedCommands.add(json)
        appendLog("Sent MODIFY_SL_TP: #$ticket SL:${cmd.sl_pips_delta}p TP:${cmd.tp_pips_delta}p")

        if (_isSimulationMode.value) {
            simulator.handleModifySlTp(cmd)
            return true
        }

        wsClient.sendModifySlTp(cmd)
        return true
    }

    fun closePosition(ticket: Long): Boolean {
        val cmd = ClosePositionCommand(ticket = ticket)

        val json = JSONObject().apply {
            put("action", "CLOSE_POSITION")
            put("ticket", cmd.ticket)
        }
        queuedCommands.add(json)
        appendLog("Sent CLOSE_POSITION: #$ticket")

        if (_isSimulationMode.value) {
            simulator.handleClosePosition(ticket)
            return true
        }

        wsClient.sendClosePosition(cmd)
        return true
    }

    fun closeAllPositions(): Boolean {
        if (_isSimulationMode.value) {
            simulator.handleCloseAll()
            return true
        }
        positions.value.forEach {
            closePosition(it.ticket)
        }
        return true
    }

    fun testPingInternalServer() {
        scope.launch(Dispatchers.IO) {
            try {
                val s = Socket("127.0.0.1", _serverPort.value)
                s.soTimeout = 1000
                val out = s.getOutputStream()
                val inp = s.getInputStream()
                out.write("GET /status HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray(Charsets.UTF_8))
                out.flush()
                val buf = ByteArray(256)
                val len = inp.read(buf)
                s.close()
                if (len > 0) {
                    appendLog("[PASS] Internal Server is healthy and responding on 127.0.0.1:${_serverPort.value}")
                }
            } catch (e: Exception) {
                appendLog("[FAIL] Test Ping error: ${e.localizedMessage}")
            }
        }
    }

    fun appendExternalLog(msg: String) {
        appendLog(msg)
    }

    private fun appendLog(msg: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $msg"
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 100) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }
}
