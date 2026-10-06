package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.repository.TradingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

object ScreenScannerManager {
    private const val TAG = "ScreenScannerManager"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler = Handler(Looper.getMainLooper())

    private val _hasProjectionPermission = MutableStateFlow(false)
    val hasProjectionPermission = _hasProjectionPermission.asStateFlow()

    private val _isScannerActive = MutableStateFlow(false)
    val isScannerActive = _isScannerActive.asStateFlow()

    private val _isTargetBoxVisible = MutableStateFlow(false)
    val isTargetBoxVisible = _isTargetBoxVisible.asStateFlow()

    private val _detectedSignal = MutableStateFlow("FLAT")
    val detectedSignal = _detectedSignal.asStateFlow()

    private val _detectedColor = MutableStateFlow(Color(0xFF1E293B))
    val detectedColor = _detectedColor.asStateFlow()

    private var lastExecutedSignal = "FLAT"
    private var displayWidth = 1080
    private var displayHeight = 2400
    private var displayDpi = 420

    // Coordinates of the floating target box on the screen
    var targetX: Int = 100
    var targetY: Int = 300
    var targetWidth: Int = 180
    var targetHeight: Int = 90

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (_isScannerActive.value) {
                performScan()
                handler.postDelayed(this, 900) // Scan every 900ms
            }
        }
    }

    fun setTargetBoxVisibility(visible: Boolean) {
        _isTargetBoxVisible.value = visible
    }

    fun updateTargetPosition(x: Int, y: Int, w: Int, h: Int) {
        targetX = x.coerceAtLeast(0)
        targetY = y.coerceAtLeast(0)
        targetWidth = w.coerceAtLeast(40)
        targetHeight = h.coerceAtLeast(30)
    }

    fun initProjection(
        context: Context,
        resultCode: Int,
        data: Intent,
        width: Int,
        height: Int,
        dpi: Int
    ) {
        try {
            displayWidth = width
            displayHeight = height
            displayDpi = dpi

            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            val projectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    _hasProjectionPermission.value = false
                    _isScannerActive.value = false
                    handler.removeCallbacks(scanRunnable)
                    TradingRepository.appendAutoTradeLog("⚠️ نشست اسکن صفحه به پایان رسید")
                }
            }
            mediaProjection?.registerCallback(projectionCallback, handler)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "SignalScannerDisplay",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            _hasProjectionPermission.value = true
            _isScannerActive.value = true
            handler.post(scanRunnable)
            Log.i(TAG, "Screen Scanner initialized successfully ($width x $height @ $dpi dpi)")
            TradingRepository.appendAutoTradeLog("🎯 اسکنر اپتیکال صفحه فعال شد ($width x $height)")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaProjection: ${e.message}", e)
            _hasProjectionPermission.value = false
            TradingRepository.appendAutoTradeLog("❌ خطا در راه‌اندازی اسکنر صفحه: ${e.message}")
        }
    }

    fun startScanning() {
        if (_hasProjectionPermission.value && !_isScannerActive.value) {
            _isScannerActive.value = true
            handler.post(scanRunnable)
            TradingRepository.appendAutoTradeLog("🟢 پایش خودکار مربع اسکنر آغاز شد")
        }
    }

    fun stopScanning() {
        _isScannerActive.value = false
        handler.removeCallbacks(scanRunnable)
        TradingRepository.appendAutoTradeLog("🔴 پایش مربع اسکنر متوقف شد")
    }

    private fun performScan() {
        val reader = imageReader ?: return
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) return

            val planes = image.planes
            if (planes.isEmpty()) return

            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride

            val centerX = (targetX + targetWidth / 2).coerceIn(0, displayWidth - 1)
            val centerY = (targetY + targetHeight / 2).coerceIn(0, displayHeight - 1)

            // Sample a 10x10 patch in the center of the target box
            var totalR = 0L
            var totalG = 0L
            var totalB = 0L
            var sampleCount = 0

            val halfPatch = 6
            for (dy in -halfPatch..halfPatch) {
                val py = (centerY + dy).coerceIn(0, displayHeight - 1)
                for (dx in -halfPatch..halfPatch) {
                    val px = (centerX + dx).coerceIn(0, displayWidth - 1)
                    val offset = py * rowStride + px * pixelStride
                    if (offset + 3 < buffer.limit()) {
                        val r = buffer.get(offset).toInt() and 0xFF
                        val g = buffer.get(offset + 1).toInt() and 0xFF
                        val b = buffer.get(offset + 2).toInt() and 0xFF
                        totalR += r
                        totalG += g
                        totalB += b
                        sampleCount++
                    }
                }
            }

            if (sampleCount > 0) {
                val avgR = (totalR / sampleCount).toInt()
                val avgG = (totalG / sampleCount).toInt()
                val avgB = (totalB / sampleCount).toInt()

                _detectedColor.value = Color(avgR, avgG, avgB)
                classifyAndDispatch(avgR, avgG, avgB)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in performScan: ${e.message}")
        } finally {
            image?.close()
        }
    }

    private fun classifyAndDispatch(r: Int, g: Int, b: Int) {
        val signal = when {
            // Bright Green (BUY in Pine Script: color.rgb(0, 200, 100))
            g > 130 && g > (r * 1.25) && g > (b * 1.25) -> "BUY"

            // Bright Orange / Amber (CLOSE in Pine Script: color.rgb(255, 140, 0))
            r > 170 && g in 80..185 && b < 95 -> "CLOSE"

            // Bright Red (SELL in Pine Script: color.rgb(240, 50, 60))
            r > 140 && r > (g * 1.35) && r > (b * 1.35) -> "SELL"

            // Dark Slate/Navy (FLAT in Pine Script: color.rgb(18, 22, 35) or color.rgb(22, 28, 42))
            r < 75 && g < 75 && b < 90 -> "FLAT"

            else -> "UNKNOWN"
        }

        _detectedSignal.value = signal

        // State Transition Trigger
        if (signal != "UNKNOWN" && signal != lastExecutedSignal) {
            Log.i(TAG, "Signal transition detected: $lastExecutedSignal -> $signal (RGB: $r, $g, $b)")
            val prev = lastExecutedSignal
            lastExecutedSignal = signal

            when (signal) {
                "BUY" -> {
                    TradingRepository.processAutoTradeSignal("BUY", "🎯 اسکنر اپتیکال صفحه (رنگ سبز)")
                }
                "SELL" -> {
                    TradingRepository.processAutoTradeSignal("SELL", "🎯 اسکنر اپتیکال صفحه (رنگ قرمز)")
                }
                "CLOSE" -> {
                    TradingRepository.processAutoTradeSignal("CLOSE", "🎯 اسکنر اپتیکال صفحه (رنگ نارنجی)")
                }
                "FLAT" -> {
                    if (prev == "BUY" || prev == "SELL") {
                        // Position closed in indicator
                        TradingRepository.appendAutoTradeLog("⚪ چارت به وضعیت FLAT برگشت")
                    }
                }
            }
        }
    }

    fun release() {
        stopScanning()
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            _hasProjectionPermission.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ScreenScannerManager: ${e.message}")
        }
    }
}
