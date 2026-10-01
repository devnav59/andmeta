package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Position
import com.example.repository.TradingRepository
import com.example.ui.components.TradeTypeBadge
import com.example.ui.theme.BuyGreen
import com.example.ui.theme.BuyGreenContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SellRed
import com.example.ui.theme.SellRedContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PositionsTab(
    modifier: Modifier = Modifier,
    isCompactOverlay: Boolean = false,
    onNavigateToNewOrder: () -> Unit = {}
) {
    val positions by TradingRepository.positions.collectAsState()
    val stats by TradingRepository.stats.collectAsState()

    var showCloseAllConfirm by remember { mutableStateOf(false) }

    if (showCloseAllConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseAllConfirm = false },
            containerColor = SlateCardElevated,
            title = { Text("Close All Positions?", color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to close all ${positions.size} open positions? This action executes immediately on MetaTrader.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        TradingRepository.closeAllPositions()
                        showCloseAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SellRed)
                ) {
                    Text("YES, CLOSE ALL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseAllConfirm = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactOverlay) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL FLOATING P/L",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            val profitColor = if (stats.totalProfit >= 0) BuyGreen else SellRed
                            Text(
                                text = String.format(Locale.US, "%+,.2f USD", stats.totalProfit),
                                fontSize = if (isCompactOverlay) 20.sp else 24.sp,
                                fontWeight = FontWeight.Black,
                                color = profitColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (positions.isNotEmpty()) {
                            Button(
                                onClick = { showCloseAllConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SellRedContainer),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("close_all_button")
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = SellRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CLOSE ALL",
                                    color = SellRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Open: ${stats.openCount} (BUY: ${stats.buyCount} | SELL: ${stats.sellCount})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Volume: ${String.format(Locale.US, "%.2f", stats.totalLots)} Lots",
                            fontSize = 11.sp,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Empty state
        if (positions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Open Positions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "There are no active orders reported by MetaTrader. Place a new order or toggle the simulator in Connection tab.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToNewOrder,
                            colors = ButtonDefaults.buttonColors(containerColor = BuyGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open New Position", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Position items
            items(positions, key = { it.ticket }) { pos ->
                PositionCard(
                    position = pos,
                    isCompact = isCompactOverlay,
                    onModifySlDelta = { delta ->
                        TradingRepository.modifySlTp(pos.ticket, slDelta = delta, tpDelta = 0.0)
                    },
                    onModifyTpDelta = { delta ->
                        TradingRepository.modifySlTp(pos.ticket, slDelta = 0.0, tpDelta = delta)
                    },
                    onBreakeven = {
                        // Delta to shift SL to Open Price
                        val pipValue = pos.pipSize
                        if (pipValue > 0) {
                            val currentSl = if (pos.sl > 0) pos.sl else pos.openPrice
                            val diffPrice = pos.openPrice - currentSl
                            val deltaPips = (diffPrice / pipValue)
                            TradingRepository.modifySlTp(pos.ticket, slDelta = deltaPips, tpDelta = 0.0)
                        }
                    },
                    onClose = {
                        TradingRepository.closePosition(pos.ticket)
                    }
                )
            }
        }
    }
}

@Composable
fun PositionCard(
    position: Position,
    isCompact: Boolean,
    onModifySlDelta: (Double) -> Unit,
    onModifyTpDelta: (Double) -> Unit,
    onBreakeven: () -> Unit,
    onClose: () -> Unit
) {
    val profitColor = if (position.profit >= 0) BuyGreen else SellRed

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .testTag("position_card_${position.ticket}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Symbol, Type, Lots, Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = position.symbol,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TradeTypeBadge(isBuy = position.isBuy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${position.formattedVolume}L",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Profit Display
                Text(
                    text = "${position.formattedProfit} USD",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = profitColor,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Row 2: Ticket, Open Price, SL, TP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${position.ticket} @ ${position.formattedPrice}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SL: ${position.formattedSl}",
                        fontSize = 11.sp,
                        color = if (position.sl > 0) SellRed else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TP: ${position.formattedTp}",
                        fontSize = 11.sp,
                        color = if (position.tp > 0) BuyGreen else TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Row 3: Pip Controls (SL & TP +/- deltas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SL Adjustment block
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SellRed
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onModifySlDelta(-5.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("-5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        IconButton(
                            onClick = { onModifySlDelta(5.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                // TP Adjustment block
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BuyGreen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onModifyTpDelta(-10.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("-10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        IconButton(
                            onClick = { onModifyTpDelta(10.0) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("+10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                // Breakeven (BE) button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onBreakeven() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "BE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                }

                // Instant Close Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = SellRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("close_ticket_${position.ticket}")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "CLOSE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
