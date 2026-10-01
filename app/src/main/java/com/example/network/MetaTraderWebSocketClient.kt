package com.example.network

import android.util.Log
import com.example.model.ClosePositionCommand
import com.example.model.ConnectionStatus
import com.example.model.ModifySlTpCommand
import com.example.model.OpenOrderCommand
import com.example.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MetaTraderWebSocketClient {

    private val tag = "MT_WS_CLIENT"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isIntentionalDisconnect = false
    private var lastUrl: String = ""

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun connect(ip: String, port: Int) {
        disconnect()
        isIntentionalDisconnect = false
        val cleanIp = ip.trim().removePrefix("ws://").removePrefix("http://")
        lastUrl = "ws://$cleanIp:$port"

        _status.value = ConnectionStatus.CONNECTING
        logEvent("Connecting to $lastUrl ...")

        val request = Request.Builder()
            .url(lastUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = ConnectionStatus.CONNECTED
                logEvent("WebSocket connected to MetaTrader EA ($lastUrl)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logEvent("WebSocket closing: $reason (code: $code)")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = ConnectionStatus.DISCONNECTED
                logEvent("WebSocket closed: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.value = ConnectionStatus.ERROR
                logEvent("Connection error: ${t.localizedMessage ?: "Unknown network failure"}")
                Log.e(tag, "WebSocket failure", t)
            }
        })
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _status.value = ConnectionStatus.DISCONNECTED
        logEvent("Disconnected from MetaTrader")
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val root = JSONObject(text)
            val action = root.optString("action", "")

            if (action == "POSITIONS_UPDATE") {
                val dataArray = root.optJSONArray("data") ?: JSONArray()
                val parsedPositions = mutableListOf<Position>()

                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val pos = Position(
                        ticket = obj.optLong("ticket", 0L),
                        symbol = obj.optString("symbol", "UNKNOWN"),
                        type = obj.optInt("type", 0),
                        volume = obj.optDouble("volume", 0.0),
                        openPrice = obj.optDouble("open_price", 0.0),
                        sl = obj.optDouble("sl", 0.0),
                        tp = obj.optDouble("tp", 0.0),
                        profit = obj.optDouble("profit", 0.0),
                        digits = obj.optInt("digits", 5),
                        pointSize = obj.optDouble("point_size", 0.00001)
                    )
                    parsedPositions.add(pos)
                }

                _positions.value = parsedPositions
            } else if (action == "ORDER_RESULT" || action == "ACK") {
                val msg = root.optString("message", "Order response received")
                logEvent("EA Response: $msg")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse message: $text", e)
            logEvent("Parse error on message: ${text.take(60)}")
        }
    }

    fun sendOpenOrder(cmd: OpenOrderCommand): Boolean {
        val json = JSONObject().apply {
            put("action", "OPEN_ORDER")
            put("symbol", cmd.symbol)
            put("type", cmd.type)
            put("volume", cmd.volume)
            put("price", cmd.price)
            put("sl_pips", cmd.sl_pips)
            put("tp_pips", cmd.tp_pips)
        }
        val sent = sendRaw(json.toString())
        if (sent) {
            logEvent("Sent OPEN_ORDER: ${cmd.type} ${cmd.volume} ${cmd.symbol}")
        }
        return sent
    }

    fun sendModifySlTp(cmd: ModifySlTpCommand): Boolean {
        val json = JSONObject().apply {
            put("action", "MODIFY_SL_TP")
            put("ticket", cmd.ticket)
            put("sl_pips_delta", cmd.sl_pips_delta)
            put("tp_pips_delta", cmd.tp_pips_delta)
        }
        val sent = sendRaw(json.toString())
        if (sent) {
            logEvent("Sent MODIFY_SL_TP: #${cmd.ticket} SL:${cmd.sl_pips_delta}p TP:${cmd.tp_pips_delta}p")
        }
        return sent
    }

    fun sendClosePosition(cmd: ClosePositionCommand): Boolean {
        val json = JSONObject().apply {
            put("action", "CLOSE_POSITION")
            put("ticket", cmd.ticket)
        }
        val sent = sendRaw(json.toString())
        if (sent) {
            logEvent("Sent CLOSE_POSITION: #${cmd.ticket}")
        }
        return sent
    }

    private fun sendRaw(json: String): Boolean {
        val ws = webSocket
        if (ws == null || _status.value != ConnectionStatus.CONNECTED) {
            logEvent("Cannot send command: WebSocket not connected")
            return false
        }
        return ws.send(json)
    }

    private fun logEvent(msg: String) {
        scope.launch {
            _events.emit(msg)
        }
    }
}
