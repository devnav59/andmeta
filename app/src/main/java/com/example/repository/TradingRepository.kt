package com.example.repository

import com.example.model.ClosePositionCommand
import com.example.model.ConnectionStatus
import com.example.model.ModifySlTpCommand
import com.example.model.OpenOrderCommand
import com.example.model.OrderType
import com.example.model.Position
import com.example.model.TradingStats
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TradingRepository {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val wsClient = MetaTraderWebSocketClient()
    private val simulator = SimulatorBridge()

    private val _serverIp = MutableStateFlow("192.168.1.100")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(listOf("System initialized. Ready to connect to MetaTrader."))
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        _isSimulationMode,
        wsClient.status
    ) { isSim, wsStatus ->
        if (isSim) ConnectionStatus.SIMULATED else wsStatus
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionStatus.DISCONNECTED)

    val positions: StateFlow<List<Position>> = combine(
        _isSimulationMode,
        wsClient.positions,
        simulator.positions
    ) { isSim, wsPos, simPos ->
        if (isSim) simPos else wsPos
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
    }

    fun setServerConfig(ip: String, port: Int) {
        _serverIp.value = ip
        _serverPort.value = port
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
        wsClient.connect(_serverIp.value, _serverPort.value)
    }

    fun disconnect() {
        if (_isSimulationMode.value) {
            simulator.stop()
            appendLog("Simulation engine stopped")
        } else {
            wsClient.disconnect()
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        _isSimulationMode.value = enabled
        if (enabled) {
            wsClient.disconnect()
            simulator.start()
            appendLog("Switched to Simulator Mode (Test MetaTrader EA without PC)")
        } else {
            simulator.stop()
            appendLog("Exited Simulator Mode. Ready for live WebSocket EA connection.")
        }
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

        return if (_isSimulationMode.value) {
            simulator.handleOpenOrder(cmd)
            true
        } else {
            wsClient.sendOpenOrder(cmd)
        }
    }

    fun modifySlTp(ticket: Long, slDelta: Double, tpDelta: Double): Boolean {
        val cmd = ModifySlTpCommand(
            ticket = ticket,
            sl_pips_delta = slDelta,
            tp_pips_delta = tpDelta
        )

        return if (_isSimulationMode.value) {
            simulator.handleModifySlTp(cmd)
            true
        } else {
            wsClient.sendModifySlTp(cmd)
        }
    }

    fun closePosition(ticket: Long): Boolean {
        val cmd = ClosePositionCommand(ticket = ticket)
        return if (_isSimulationMode.value) {
            simulator.handleClosePosition(ticket)
            true
        } else {
            wsClient.sendClosePosition(cmd)
        }
    }

    fun closeAllPositions(): Boolean {
        return if (_isSimulationMode.value) {
            simulator.handleCloseAll()
            true
        } else {
            var allSent = true
            positions.value.forEach {
                val sent = wsClient.sendClosePosition(ClosePositionCommand(ticket = it.ticket))
                if (!sent) allSent = false
            }
            allSent
        }
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
