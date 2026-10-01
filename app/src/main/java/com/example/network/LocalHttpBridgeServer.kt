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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

/**
 * Built-in embedded HTTP server inside the Android app.
 * Allows MetaTrader 4 and MetaTrader 5 EAs (using standard WebRequest)
 * to communicate directly with the phone over local Wi-Fi without any third-party PC bridge!
 */
class LocalHttpBridgeServer {

    private val tag = "LOCAL_HTTP_SERVER"
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start(port: Int = 8080) {
        stop()
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Log.i(tag, "Local HTTP Bridge Server started on port $port")
                TradingRepository.appendExternalLog("Embedded Server started on port $port (Ready for direct MT4/MT5 WebRequest)")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(tag, "Server error", e)
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
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out: OutputStream = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            var contentLength = 0

            // Read headers
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line.substring(15).trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }

            // Read JSON body
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(chars, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(chars, 0, totalRead)
            } else ""

            if (requestLine.contains("/api/positions") && body.isNotEmpty()) {
                val pendingCommandsJson = processPositionsUpdate(body)
                val responseBody = "{\"status\":\"ok\",\"commands\":$pendingCommandsJson}"
                val responseBytes = responseBody.toByteArray(Charsets.UTF_8)

                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: ${responseBytes.size}\r\n" +
                        "Connection: close\r\n\r\n" +
                        responseBody

                out.write(response.toByteArray(Charsets.UTF_8))
                out.flush()
            } else {
                val notFound = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 2\r\n\r\nOK"
                out.write(notFound.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        } catch (e: Exception) {
            Log.e(tag, "Client error", e)
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
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
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to parse positions JSON", e)
        }

        // Return and drain any queued commands
        return TradingRepository.drainPendingCommandsJson()
    }
}
