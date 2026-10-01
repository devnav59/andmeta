package com.example.network

import android.util.Log
import com.example.model.Position
import com.example.repository.TradingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64

/**
 * Universal Multi-Protocol Bridge Server (Port 8080).
 * Specifically engineered for MetaTrader running inside Winlator on the SAME Android phone,
 * as well as remote PC MetaTrader over Wi-Fi.
 *
 * Supported Protocols on a single port:
 * 1. Raw TCP JSON streaming (MQL5 SocketConnect - Best & fastest for Winlator!)
 * 2. HTTP WebRequest (POST /api/positions - Native MT4 & MT5 WebRequest)
 * 3. RFC 6455 WebSocket (Bidirectional real-time stream for Android OkHttp & external clients)
 */
class MultiProtocolBridgeServer {

    private val tag = "MULTI_BRIDGE_SERVER"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val activeWsClients = mutableListOf<Socket>()

    fun start(port: Int = 8080) {
        stop()
        serverJob = scope.launch {
            try {
                // Bind to 0.0.0.0 (all interfaces including 127.0.0.1, Wi-Fi, cellular, hotspot)
                serverSocket = ServerSocket(port)
                Log.i(tag, "Multi-Protocol Server listening on 0.0.0.0:$port")
                TradingRepository.appendExternalLog("Server running on port $port (Ready for Winlator & Wi-Fi)")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(tag, "Server error on port $port", e)
                    TradingRepository.appendExternalLog("Server bind error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        synchronized(activeWsClients) {
            activeWsClients.forEach { try { it.close() } catch (_: Exception) {} }
            activeWsClients.clear()
        }
    }

    private fun handleClient(socket: Socket) {
        val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
        try {
            val input: InputStream = socket.getInputStream()
            val output: OutputStream = socket.getOutputStream()

            // Peek or read initial buffer to detect protocol
            val buffer = ByteArray(8192)
            val bytesRead = input.read(buffer)
            if (bytesRead <= 0) {
                socket.close()
                return
            }

            val headerStr = String(buffer, 0, bytesRead, Charsets.UTF_8)

            when {
                // Case 1: WebSocket Handshake Request (HTTP GET with Upgrade: websocket)
                headerStr.startsWith("GET ") && headerStr.contains("Upgrade: websocket", ignoreCase = true) -> {
                    handleWebSocketHandshake(socket, headerStr, output, input)
                }

                // Case 2: HTTP WebRequest (POST /api/positions from MT4/MT5 WebRequest)
                headerStr.startsWith("POST ") || headerStr.startsWith("GET ") -> {
                    handleHttpRequest(headerStr, bytesRead, input, output)
                    socket.close()
                }

                // Case 3: Raw TCP JSON from MQL5 SocketConnect (Winlator Direct Socket)
                headerStr.trimStart().startsWith("{") -> {
                    handleRawTcpJson(headerStr, bytesRead, input, output, socket)
                }

                else -> {
                    // Fallback try parsing as JSON
                    handleRawTcpJson(headerStr, bytesRead, input, output, socket)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Client connection error from $clientIp", e)
        }
    }

    // -------------------------------------------------------------
    // Protocol 1: Raw TCP JSON (Ideal for Winlator MQL5 SocketConnect)
    // -------------------------------------------------------------
    private fun handleRawTcpJson(
        firstChunk: String,
        initialLen: Int,
        input: InputStream,
        output: OutputStream,
        socket: Socket
    ) {
        var completeJson = firstChunk.trim()
        val pendingCmds = processPositionsUpdate(completeJson)

        // Reply immediately with pending commands
        val reply = "{\"status\":\"ok\",\"commands\":$pendingCmds}\n"
        output.write(reply.toByteArray(Charsets.UTF_8))
        output.flush()

        // Keep socket alive for streaming if client sends more packets
        val streamBuf = ByteArray(8192)
        try {
            while (socket.isConnected && !socket.isClosed) {
                val read = input.read(streamBuf)
                if (read <= 0) break
                val chunk = String(streamBuf, 0, read, Charsets.UTF_8).trim()
                if (chunk.isNotEmpty()) {
                    val cmds = processPositionsUpdate(chunk)
                    val resp = "{\"status\":\"ok\",\"commands\":$cmds}\n"
                    output.write(resp.toByteArray(Charsets.UTF_8))
                    output.flush()
                }
            }
        } catch (_: Exception) {} finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Protocol 2: HTTP WebRequest (Standard MT4/MT5 WebRequest)
    // -------------------------------------------------------------
    private fun handleHttpRequest(
        headerStr: String,
        bytesRead: Int,
        input: InputStream,
        output: OutputStream
    ) {
        var contentLength = 0
        val lines = headerStr.split("\r\n")
        for (line in lines) {
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substring(15).trim().toIntOrNull() ?: 0
            }
        }

        val headerEnd = headerStr.indexOf("\r\n\r\n")
        val bodyInHeader = if (headerEnd >= 0 && headerEnd + 4 < bytesRead) {
            headerStr.substring(headerEnd + 4)
        } else ""

        var body = bodyInHeader
        val remainingBytes = contentLength - bodyInHeader.toByteArray(Charsets.UTF_8).size
        if (remainingBytes > 0) {
            val extraBuf = ByteArray(remainingBytes)
            var totalExtra = 0
            while (totalExtra < remainingBytes) {
                val r = input.read(extraBuf, totalExtra, remainingBytes - totalExtra)
                if (r <= 0) break
                totalExtra += r
            }
            body += String(extraBuf, 0, totalExtra, Charsets.UTF_8)
        }

        val pendingCmds = processPositionsUpdate(body)
        val responseBody = "{\"status\":\"ok\",\"commands\":$pendingCmds}"
        val responseBytes = responseBody.toByteArray(Charsets.UTF_8)

        val httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${responseBytes.size}\r\n" +
                "Connection: close\r\n\r\n" +
                responseBody

        output.write(httpResponse.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    // -------------------------------------------------------------
    // Protocol 3: WebSocket RFC 6455 Handshake & Framing
    // -------------------------------------------------------------
    private fun handleWebSocketHandshake(
        socket: Socket,
        headerStr: String,
        output: OutputStream,
        input: InputStream
    ) {
        val keyRegex = "Sec-WebSocket-Key:\\s*([^\\r\\n]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = keyRegex.find(headerStr)
        val key = match?.groupValues?.get(1)?.trim() ?: return

        val acceptKey = generateWebSocketAcceptKey(key)
        val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"

        output.write(handshakeResponse.toByteArray(Charsets.UTF_8))
        output.flush()

        synchronized(activeWsClients) {
            activeWsClients.add(socket)
        }
        TradingRepository.appendExternalLog("WebSocket client connected to local server")

        // Broadcast current positions immediately
        broadcastPositionsToWs(TradingRepository.positions.value)

        // Read WebSocket frames
        try {
            while (socket.isConnected && !socket.isClosed) {
                val b1 = input.read()
                if (b1 == -1) break
                val b2 = input.read()
                if (b2 == -1) break

                val opcode = b1 and 0x0F
                if (opcode == 0x08) break // Close frame

                val isMasked = (b2 and 0x80) != 0
                var payloadLen = (b2 and 0x7F).toLong()

                if (payloadLen == 126L) {
                    val high = input.read()
                    val low = input.read()
                    if (high == -1 || low == -1) break
                    payloadLen = ((high shl 8) or low).toLong()
                } else if (payloadLen == 127L) {
                    var len = 0L
                    for (i in 0 until 8) {
                        val b = input.read()
                        if (b == -1) break
                        len = (len shl 8) or (b.toLong() and 0xFF)
                    }
                    payloadLen = len
                }

                val mask = ByteArray(4)
                if (isMasked) {
                    var readMask = 0
                    while (readMask < 4) {
                        val r = input.read(mask, readMask, 4 - readMask)
                        if (r == -1) break
                        readMask += r
                    }
                }

                val payload = ByteArray(payloadLen.toInt())
                var totalPayloadRead = 0
                while (totalPayloadRead < payloadLen.toInt()) {
                    val r = input.read(payload, totalPayloadRead, payloadLen.toInt() - totalPayloadRead)
                    if (r == -1) break
                    totalPayloadRead += r
                }

                if (isMasked) {
                    for (i in payload.indices) {
                        payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
                    }
                }

                val text = String(payload, Charsets.UTF_8)
                processClientCommand(text)
            }
        } catch (_: Exception) {} finally {
            synchronized(activeWsClients) {
                activeWsClients.remove(socket)
            }
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun generateWebSocketAcceptKey(key: String): String {
        val guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val md = MessageDigest.getInstance("SHA-1")
        val hash = md.digest((key + guid).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }

    fun broadcastPositionsToWs(positions: List<Position>) {
        if (activeWsClients.isEmpty()) return
        try {
            val root = JSONObject()
            root.put("action", "POSITIONS_UPDATE")
            val array = JSONArray()
            for (p in positions) {
                val obj = JSONObject().apply {
                    put("ticket", p.ticket)
                    put("symbol", p.symbol)
                    put("type", p.type)
                    put("volume", p.volume)
                    put("open_price", p.openPrice)
                    put("sl", p.sl)
                    put("tp", p.tp)
                    put("profit", p.profit)
                    put("digits", p.digits)
                    put("point_size", p.pointSize)
                }
                array.put(obj)
            }
            root.put("data", array)

            val jsonStr = root.toString()
            val payload = jsonStr.toByteArray(Charsets.UTF_8)
            val frame = encodeWsTextFrame(payload)

            synchronized(activeWsClients) {
                val dead = mutableListOf<Socket>()
                for (s in activeWsClients) {
                    try {
                        s.getOutputStream().write(frame)
                        s.getOutputStream().flush()
                    } catch (_: Exception) {
                        dead.add(s)
                    }
                }
                activeWsClients.removeAll(dead)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to broadcast WS", e)
        }
    }

    private fun encodeWsTextFrame(payload: ByteArray): ByteArray {
        val len = payload.size
        val header = when {
            len <= 125 -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 65535 -> byteArrayOf(
                0x81.toByte(),
                126.toByte(),
                ((len shr 8) and 0xFF).toByte(),
                (len and 0xFF).toByte()
            )
            else -> {
                val b = ByteArray(10)
                b[0] = 0x81.toByte()
                b[1] = 127.toByte()
                for (i in 0 until 8) {
                    b[9 - i] = ((len.toLong() shr (i * 8)) and 0xFF).toByte()
                }
                b
            }
        }
        return header + payload
    }

    private fun processPositionsUpdate(jsonStr: String): String {
        try {
            val root = JSONObject(jsonStr)
            val action = root.optString("action")
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

                TradingRepository.updatePositionsFromHttp(parsedPositions)
                broadcastPositionsToWs(parsedPositions)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse positions JSON: ${jsonStr.take(100)}", e)
        }

        return TradingRepository.drainPendingCommandsJson()
    }

    private fun processClientCommand(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val action = root.optString("action")
            if (action in listOf("OPEN_ORDER", "MODIFY_SL_TP", "CLOSE_POSITION")) {
                TradingRepository.enqueueCommand(root)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing client command", e)
        }
    }
}
