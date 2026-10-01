package com.example.network

import com.example.model.ClosePositionCommand
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
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

/**
 * Built-in local simulator that mimics a live MetaTrader 4/5 EA instance.
 * Allows full interactive testing of orders, SL/TP delta modifications,
 * and live P/L tick updates directly in the app.
 */
class SimulatorBridge {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null
    private val nextTicket = AtomicLong(5482910)

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun start() {
        if (_positions.value.isEmpty()) {
            _positions.value = listOf(
                Position(
                    ticket = nextTicket.getAndIncrement(),
                    symbol = "EURUSD",
                    type = 0, // BUY
                    volume = 0.50,
                    openPrice = 1.08520,
                    sl = 1.08220,
                    tp = 1.09120,
                    profit = 28.50,
                    digits = 5,
                    pointSize = 0.00001
                ),
                Position(
                    ticket = nextTicket.getAndIncrement(),
                    symbol = "XAUUSD",
                    type = 1, // SELL
                    volume = 0.20,
                    openPrice = 2650.40,
                    sl = 2660.00,
                    tp = 2635.00,
                    profit = -14.20,
                    digits = 2,
                    pointSize = 0.01
                ),
                Position(
                    ticket = nextTicket.getAndIncrement(),
                    symbol = "GBPUSD",
                    type = 0, // BUY
                    volume = 0.30,
                    openPrice = 1.30150,
                    sl = 1.29850,
                    tp = 1.30750,
                    profit = 42.10,
                    digits = 5,
                    pointSize = 0.00001
                )
            )
        }

        tickerJob?.cancel()
        tickerJob = scope.launch {
            emitLog("Simulator EA started on port 8080 (Mock MetaTrader)")
            while (isActive) {
                delay(1200)
                updateRandomPriceTicks()
            }
        }
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        emitLog("Simulator EA stopped")
    }

    private fun updateRandomPriceTicks() {
        val current = _positions.value.toMutableList()
        val updated = current.map { pos ->
            val deltaProfit = (Random.nextDouble() - 0.48) * (pos.volume * 8.0)
            val newProfit = ((pos.profit + deltaProfit) * 100).toInt() / 100.0
            pos.copy(profit = newProfit)
        }
        _positions.value = updated
    }

    fun handleOpenOrder(cmd: OpenOrderCommand) {
        val digits = when {
            cmd.symbol.contains("JPY") -> 3
            cmd.symbol.contains("XAU") || cmd.symbol.contains("GOLD") -> 2
            cmd.symbol.contains("BTC") -> 2
            else -> 5
        }
        val pointSize = when (digits) {
            5 -> 0.00001
            3 -> 0.001
            2 -> 0.01
            else -> 0.0001
        }
        val pipMultiplier = if (digits == 3 || digits == 5) 10.0 else 1.0
        val pipValue = pointSize * pipMultiplier

        val basePrice = when {
            cmd.price > 0.0 -> cmd.price
            cmd.symbol.contains("XAU") -> 2650.00
            cmd.symbol.contains("BTC") -> 64500.00
            cmd.symbol.contains("JPY") -> 148.50
            else -> 1.08500
        }

        val isBuy = cmd.type.startsWith("BUY")
        val slPrice = if (cmd.sl_pips > 0) {
            if (isBuy) basePrice - (cmd.sl_pips * pipValue) else basePrice + (cmd.sl_pips * pipValue)
        } else 0.0

        val tpPrice = if (cmd.tp_pips > 0) {
            if (isBuy) basePrice + (cmd.tp_pips * pipValue) else basePrice - (cmd.tp_pips * pipValue)
        } else 0.0

        val newPos = Position(
            ticket = nextTicket.getAndIncrement(),
            symbol = cmd.symbol.uppercase(),
            type = if (isBuy) 0 else 1,
            volume = cmd.volume,
            openPrice = basePrice,
            sl = slPrice,
            tp = tpPrice,
            profit = 0.0,
            digits = digits,
            pointSize = pointSize
        )

        _positions.value = _positions.value + newPos
        emitLog("Executed ${cmd.type} ${cmd.volume} ${cmd.symbol} @ $basePrice (Ticket: #${newPos.ticket})")
    }

    fun handleModifySlTp(cmd: ModifySlTpCommand) {
        val list = _positions.value.toMutableList()
        val index = list.indexOfFirst { it.ticket == cmd.ticket }
        if (index != -1) {
            val old = list[index]
            val pipValue = old.pipSize
            val isBuy = old.isBuy

            // Positive delta means add pips, negative means reduce
            // For Buy: moving SL up is +pips, moving TP up is +pips
            // For Sell: moving SL down is +pips, moving TP down is +pips
            val slShift = if (isBuy) cmd.sl_pips_delta * pipValue else -(cmd.sl_pips_delta * pipValue)
            val tpShift = if (isBuy) cmd.tp_pips_delta * pipValue else -(cmd.tp_pips_delta * pipValue)

            val newSl = if (old.sl > 0) old.sl + slShift else if (cmd.sl_pips_delta > 0) old.openPrice + slShift else 0.0
            val newTp = if (old.tp > 0) old.tp + tpShift else if (cmd.tp_pips_delta > 0) old.openPrice + tpShift else 0.0

            list[index] = old.copy(
                sl = (newSl * 100000).toLong() / 100000.0,
                tp = (newTp * 100000).toLong() / 100000.0
            )
            _positions.value = list
            emitLog("Modified #${cmd.ticket} SL delta: ${cmd.sl_pips_delta}p, TP delta: ${cmd.tp_pips_delta}p")
        }
    }

    fun handleClosePosition(ticket: Long) {
        val pos = _positions.value.find { it.ticket == ticket }
        if (pos != null) {
            _positions.value = _positions.value.filterNot { it.ticket == ticket }
            emitLog("Closed position #${ticket} (${pos.symbol} Profit: ${pos.formattedProfit})")
        }
    }

    fun handleCloseAll() {
        val count = _positions.value.size
        _positions.value = emptyList()
        emitLog("Closed all $count open positions")
    }

    private fun emitLog(msg: String) {
        scope.launch {
            _events.emit(msg)
        }
    }
}
