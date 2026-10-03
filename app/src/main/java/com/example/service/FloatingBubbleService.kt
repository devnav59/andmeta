package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.repository.TradingRepository
import com.example.ui.overlay.FloatingBubbleView
import com.example.ui.overlay.FloatingPanelWindow
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.hypot

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private val bubbleLifecycleOwner = ServiceLifecycleOwner()
    private val panelLifecycleOwner = ServiceLifecycleOwner()

    private var bubbleView: ComposeView? = null
    private var panelView: ComposeView? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private var isPanelShowing = false

    companion object {
        const val CHANNEL_ID = "mt_floating_bubble_channel"
        const val NOTIFICATION_ID = 1010
        const val ACTION_START = "ACTION_START_BUBBLE"
        const val ACTION_STOP = "ACTION_STOP_BUBBLE"

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        TradingRepository.setServiceRunning(true)
        bubbleLifecycleOwner.handleOnStart()
        bubbleLifecycleOwner.handleOnResume()
        panelLifecycleOwner.handleOnStart()
        panelLifecycleOwner.handleOnResume()

        if (Settings.canDrawOverlays(this)) {
            initBubbleView()
            initPanelView()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun initBubbleView() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = dpToPx(64)

        bubbleParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 250
        }

        bubbleView = ComposeView(this).apply {
            bubbleLifecycleOwner.attachTo(this)
            setContent {
                MyApplicationTheme {
                    FloatingBubbleView(
                        onClick = { togglePanel() }
                    )
                }
            }

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams.x
                        initialY = bubbleParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        bubbleParams.x = initialX + deltaX
                        bubbleParams.y = initialY + deltaY
                        windowManager.updateViewLayout(this, bubbleParams)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = event.rawX - initialTouchX
                        val diffY = event.rawY - initialTouchY
                        if (hypot(diffX.toDouble(), diffY.toDouble()) < 15) {
                            togglePanel()
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun initPanelView() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val dm = resources.displayMetrics
        val initialWidth = dpToPx(320).coerceAtMost((dm.widthPixels * 0.95).toInt())
        val initialHeight = dpToPx(380).coerceAtMost((dm.heightPixels * 0.70).toInt())

        panelParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ((dm.widthPixels - initialWidth) / 2).coerceAtLeast(dpToPx(10))
            y = dpToPx(60)
        }

        panelView = ComposeView(this).apply {
            panelLifecycleOwner.attachTo(this)
            setContent {
                MyApplicationTheme {
                    FloatingPanelWindow(
                        onMinimize = { hidePanel() },
                        onCloseService = { stopSelf() },
                        onOpenFullApp = {
                            val launchIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            context.startActivity(launchIntent)
                        },
                        onDragDelta = { dx, dy ->
                            panelParams.x = (panelParams.x + dx.toInt()).coerceIn(0, dm.widthPixels - dpToPx(80))
                            panelParams.y = (panelParams.y + dy.toInt()).coerceIn(0, dm.heightPixels - dpToPx(80))
                            if (panelView?.isAttachedToWindow == true) {
                                windowManager.updateViewLayout(panelView, panelParams)
                            }
                        },
                        onResizeDelta = { dw, dh ->
                            val newW = (panelParams.width + dw.toInt()).coerceIn(dpToPx(240), dm.widthPixels)
                            val newH = (panelParams.height + dh.toInt()).coerceIn(dpToPx(260), dm.heightPixels)
                            panelParams.width = newW
                            panelParams.height = newH
                            if (panelView?.isAttachedToWindow == true) {
                                windowManager.updateViewLayout(panelView, panelParams)
                            }
                        },
                        onToggleSizePreset = { preset ->
                            when (preset) {
                                0 -> { // Small / Compact
                                    panelParams.width = dpToPx(270).coerceAtMost(dm.widthPixels)
                                    panelParams.height = dpToPx(320).coerceAtMost(dm.heightPixels)
                                }
                                1 -> { // Medium / Normal
                                    panelParams.width = dpToPx(320).coerceAtMost(dm.widthPixels)
                                    panelParams.height = dpToPx(390).coerceAtMost(dm.heightPixels)
                                }
                                2 -> { // Large
                                    panelParams.width = dpToPx(360).coerceAtMost(dm.widthPixels)
                                    panelParams.height = dpToPx(480).coerceAtMost(dm.heightPixels)
                                }
                            }
                            if (panelView?.isAttachedToWindow == true) {
                                windowManager.updateViewLayout(panelView, panelParams)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun togglePanel() {
        if (isPanelShowing) {
            hidePanel()
        } else {
            showPanel()
        }
    }

    private fun showPanel() {
        if (isPanelShowing || panelView == null) return
        try {
            windowManager.addView(panelView, panelParams)
            isPanelShowing = true
            // Dim/hide the bubble while full panel is open for clean screen space
            bubbleView?.visibility = View.GONE
        } catch (_: Exception) {}
    }

    private fun hidePanel() {
        if (!isPanelShowing || panelView == null) return
        try {
            windowManager.removeView(panelView)
            isPanelShowing = false
            bubbleView?.visibility = View.VISIBLE
        } catch (_: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MetaTrader Floating Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls MetaTrader floating window on top of applications"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MetaTrader Floating Bubble Active")
            .setContentText("Overlay control panel is ready on screen")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_launcher_foreground, "STOP OVERLAY", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        TradingRepository.setServiceRunning(false)
        try {
            if (isPanelShowing && panelView != null) {
                windowManager.removeView(panelView)
            }
            if (bubbleView != null) {
                windowManager.removeView(bubbleView)
            }
        } catch (_: Exception) {}

        bubbleLifecycleOwner.handleOnDestroy()
        panelLifecycleOwner.handleOnDestroy()
    }
}
