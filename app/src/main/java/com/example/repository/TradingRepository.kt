package com.example.repository

import com.example.model.ClosePositionCommand
import com.example.model.ConnectionStatus
import com.example.model.ModifySlTpCommand
import com.example.model.OpenOrderCommand
import com.example.model.OrderType
import com.example.model.Position
import com.example.model.TradingStats
import com.example.network.LocalHttpBridgeServer
import com.example.network.MetaTraderWebSocketClient
import com.example.network.SimulatorBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Collections

object TradingRepository {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val wsClient = MetaTraderWebSocketClient()
    private val simulator = SimulatorBridge()
    private val localHttpServer = LocalHttpBridgeServer()

    private val _serverIp = MutableStateFlow("192.168.1.100")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _httpStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    private val _httpPositions = MutableStateFlow<List<Position>>(emptyList())

    private val queuedCommands = Collections.synchronizedList(mutableListOf<JSONObject>())

    private val _logs = MutableStateFlow<List<String>>(listOf("System initialized. Ready to connect to MetaTrader."))
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        _isSimulationMode,
        wsClient.status,
        _httpStatus
    ) { isSim, wsStatus, httpStatus ->
        when {
            isSim -> ConnectionStatus.SIMULATED
            wsStatus == ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
            httpStatus == ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
            wsStatus == ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
            wsStatus == ConnectionStatus.ERROR -> ConnectionStatus.ERROR
            else -> ConnectionStatus.DISCONNECTED
        }
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionStatus.DISCONNECTED)

    val positions: StateFlow<List<Position>> = combine(
        _isSimulationMode,
        wsClient.positions,
        simulator.positions,
        _httpPositions
    ) { isSim, wsPos, simPos, httpPos ->
        when {
            isSim -> simPos
            httpPos.isNotEmpty() -> httpPos
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
        // Start local server to accept incoming WebRequests from MetaTrader
        localHttpServer.start(_serverPort.value)
    }

    fun setServerConfig(ip: String, port: Int) {
        _serverIp.value = ip
        _serverPort.value = port
        localHttpServer.start(port)
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
        localHttpServer.start(_serverPort.value)
        wsClient.connect(_serverIp.value, _serverPort.value)
    }

    fun disconnect() {
        if (_isSimulationMode.value) {
            simulator.stop()
            appendLog("Simulation engine stopped")
        } else {
            wsClient.disconnect()
            _httpStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (enabled) {
            wsClient.disconnect()
            _httpStatus.value = ConnectionStatus.DISCONNECTED
            simulator.start()
            appendLog("Switched to Simulator Mode (Test MetaTrader EA without PC)")
        } else {
            simulator.stop()
            appendLog("Exited Simulator Mode. Ready for live EA connection.")
        }
    }

    fun updatePositionsFromHttp(list: List<Position>) {
        _httpPositions.value = list
        _httpStatus.value = ConnectionStatus.CONNECTED
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

        // Queue command for HTTP EA polling
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

        return if (_isSimulationMode.value) {
            simulator.handleOpenOrder(cmd)
            true
        } else {
            wsClient.sendOpenOrder(cmd) || true
        }
    }

    fun modifySlTp(ticket: Long, slDelta: Double, tpDelta: Double): Boolean {
        val cmd = ModifySlTpCommand(
            ticket = ticket,
            sl_pips_delta = slDelta,
            tp_pips_delta = tpDelta
        )

        // Queue command for HTTP EA polling
        val json = JSONObject().apply {
            put("action", "MODIFY_SL_TP")
            put("ticket", cmd.ticket)
            put("sl_pips_delta", cmd.sl_pips_delta)
            put("tp_pips_delta", cmd.tp_pips_delta)
        }
        queuedCommands.add(json)

        return if (_isSimulationMode.value) {
            simulator.handleModifySlTp(cmd)
            true
        } else {
            wsClient.sendModifySlTp(cmd) || true
        }
    }

    fun closePosition(ticket: Long): Boolean {
        val cmd = ClosePositionCommand(ticket = ticket)

        // Queue command for HTTP EA polling
        val json = JSONObject().apply {
            put("action", "CLOSE_POSITION")
            put("ticket", cmd.ticket)
        }
        queuedCommands.add(json)

        return if (_isSimulationMode.value) {
            simulator.handleClosePosition(ticket)
            true
        } else {
            wsClient.sendClosePosition(cmd) || true
        }
    }

    fun closeAllPositions(): Boolean {
        return if (_isSimulationMode.value) {
            simulator.handleCloseAll()
            true
        } else {
            positions.value.forEach {
                closePosition(it.ticket)
            }
            true
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
