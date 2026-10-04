package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.repository.TradingRepository

class TradingViewNotificationListener : NotificationListenerService() {

    private val tag = "TV_NOTIF_LISTENER"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName?.lowercase() ?: ""
        // Check if notification is from TradingView app
        if (!pkg.contains("tradingview")) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullContent = "$title $text $bigText".trim()
        Log.d(tag, "TradingView Notification intercepted: $fullContent")

        TradingRepository.processAutoTradeSignal(fullContent, source = "TradingView App Notification")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(tag, "TradingViewNotificationListener connected successfully")
        TradingRepository.setNotificationListenerActive(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i(tag, "TradingViewNotificationListener disconnected")
        TradingRepository.setNotificationListenerActive(false)
    }
}
