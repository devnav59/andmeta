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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isIntentionalDisconnect = false
    private var lastUrl: String = ""
    private var httpBaseUrl: String = ""
    private var pollJob: Job? = null

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun connect(ip: String, port: Int) {
        disconnect()
        isIntentionalDisconnect = false
        val cleanIp = ip.trim().removePrefix("ws://").removePrefix("http://").removePrefix("https://")
        lastUrl = "ws://$cleanIp:$port"
        httpBaseUrl = "http://$cleanIp:$port"

        _status.value = ConnectionStatus.CONNECTING
        logEvent("Connecting to VPS ($cleanIp:$port)...")

        // 1. Try WebSocket connection
        val request = Request.Builder()
            .url(lastUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = ConnectionStatus.CONNECTED
                stopHttpPolling()
                logEvent("WebSocket connected to VPS ($lastUrl)")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logEvent("WebSocket closing: $reason (code: $code)")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isIntentionalDisconnect) {
                    startHttpPolling()
                } else {
                    _status.value = ConnectionStatus.DISCONNECTED
                    logEvent("WebSocket closed: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket failure, falling back to HTTP: ${t.localizedMessage}")
                if (!isIntentionalDisconnect) {
                    // Automatically fallback to HTTP polling (supports bridge.ps1)
                    startHttpPolling()
                } else {
                    _status.value = ConnectionStatus.ERROR
                    logEvent("VPS Connection Failed: ${t.localizedMessage}")
                }
            }
        })
    }

    private fun startHttpPolling() {
        if (pollJob != null && pollJob?.isActive == true) return
        logEvent("Switching to HTTP polling mode (http://$httpBaseUrl/api/positions)...")

        pollJob = scope.launch {
            var consecutiveFailures = 0
            while (isActive && !isIntentionalDisconnect) {
                try {
                    val req = Request.Builder()
                        .url("$httpBaseUrl/api/positions")
                        .get()
                        .build()

                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        if (body.isNotEmpty()) {
                            handleIncomingMessage(body)
                            if (_status.value != ConnectionStatus.CONNECTED) {
                                _status.value = ConnectionStatus.CONNECTED
                                logEvent("Connected to VPS Bridge via HTTP (Live)")
                            }
                            consecutiveFailures = 0
                        }
                    } else {
                        consecutiveFailures++
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures > 3) {
                        _status.value = ConnectionStatus.ERROR
                        logEvent("HTTP Poll failed: ${e.localizedMessage ?: "Connection refused"} (Check Firewall)")
                    }
                }
                delay(1200)
            }
        }
    }

    private fun stopHttpPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        stopHttpPolling()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _status.value = ConnectionStatus.DISCONNECTED
        logEvent("Disconnected from VPS")
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val root = JSONObject(text)
            val action = root.optString("action", "")

            if (action == "POSITIONS_UPDATE" || root.has("data")) {
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
        }.toString()

        return sendCommandJson(json, "OPEN_ORDER ${cmd.type} ${cmd.volume} ${cmd.symbol}")
    }

    fun sendModifySlTp(cmd: ModifySlTpCommand): Boolean {
        val json = JSONObject().apply {
            put("action", "MODIFY_SL_TP")
            put("ticket", cmd.ticket)
            put("sl_pips_delta", cmd.sl_pips_delta)
            put("tp_pips_delta", cmd.tp_pips_delta)
        }.toString()

        return sendCommandJson(json, "MODIFY_SL_TP #${cmd.ticket}")
    }

    fun sendClosePosition(cmd: ClosePositionCommand): Boolean {
        val json = JSONObject().apply {
            put("action", "CLOSE_POSITION")
            put("ticket", cmd.ticket)
        }.toString()

        return sendCommandJson(json, "CLOSE_POSITION #${cmd.ticket}")
    }

    private fun sendCommandJson(json: String, logLabel: String): Boolean {
        var sent = false
        // Try WebSocket first
        if (webSocket != null && _status.value == ConnectionStatus.CONNECTED) {
            sent = webSocket?.send(json) ?: false
        }

        // Also send via HTTP POST for bridge.ps1 compatibility
        if (httpBaseUrl.isNotEmpty()) {
            scope.launch {
                try {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val req = Request.Builder()
                        .url("$httpBaseUrl/api/command")
                        .post(json.toRequestBody(mediaType))
                        .build()
                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        logEvent("Sent to VPS: $logLabel")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "HTTP command failed: $e")
                }
            }
        }

        return sent || httpBaseUrl.isNotEmpty()
    }

    private fun logEvent(event: String) {
        scope.launch {
            _events.emit(event)
        }
    }
}
