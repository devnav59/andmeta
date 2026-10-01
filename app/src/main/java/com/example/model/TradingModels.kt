package com.example.model

import java.util.Locale

/**
 * Representation of a MetaTrader open position
 */
data class Position(
    val ticket: Long,
    val symbol: String,
    val type: Int, // 0 = Buy, 1 = Sell
    val volume: Double,
    val openPrice: Double,
    val sl: Double,
    val tp: Double,
    val profit: Double,
    val digits: Int = 5,
    val pointSize: Double = 0.00001
) {
    val isBuy: Boolean get() = type == 0
    val isSell: Boolean get() = type == 1
    val typeName: String get() = if (isBuy) "BUY" else "SELL"

    // Pip calculation rule:
    // 5 digits / 3 digits: 1 Pip = 10 Points
    // 4 digits / 2 digits: 1 Pip = 1 Point
    val pipMultiplier: Double get() = if (digits == 3 || digits == 5) 10.0 else 1.0
    val pipSize: Double get() = pointSize * pipMultiplier

    val formattedPrice: String
        get() = String.format(Locale.US, "%.${digits}f", openPrice)

    val formattedSl: String
        get() = if (sl <= 0.0) "--" else String.format(Locale.US, "%.${digits}f", sl)

    val formattedTp: String
        get() = if (tp <= 0.0) "--" else String.format(Locale.US, "%.${digits}f", tp)

    val formattedProfit: String
        get() = String.format(Locale.US, "%+.2f", profit)

    val formattedVolume: String
        get() = String.format(Locale.US, "%.2f", volume)
}

enum class OrderType(val displayName: String, val isPending: Boolean) {
    BUY("BUY (Market)", false),
    SELL("SELL (Market)", false),
    BUY_LIMIT("BUY LIMIT", true),
    SELL_LIMIT("SELL LIMIT", true),
    BUY_STOP("BUY STOP", true),
    SELL_STOP("SELL STOP", true)
}

enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected (Live)"),
    SIMULATED("Simulated Mode"),
    ERROR("Connection Failed")
}

data class TradingStats(
    val totalProfit: Double = 0.0,
    val totalLots: Double = 0.0,
    val openCount: Int = 0,
    val buyCount: Int = 0,
    val sellCount: Int = 0
)

// Command models matching user's exact JSON specification
data class OpenOrderCommand(
    val action: String = "OPEN_ORDER",
    val symbol: String,
    val type: String, // BUY, SELL, BUY_LIMIT, etc.
    val volume: Double,
    val price: Double = 0.0,
    val sl_pips: Double = 0.0,
    val tp_pips: Double = 0.0
)

data class ModifySlTpCommand(
    val action: String = "MODIFY_SL_TP",
    val ticket: Long,
    val sl_pips_delta: Double,
    val tp_pips_delta: Double
)

data class ClosePositionCommand(
    val action: String = "CLOSE_POSITION",
    val ticket: Long
)
