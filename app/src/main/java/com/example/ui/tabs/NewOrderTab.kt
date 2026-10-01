package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.OrderType
import com.example.repository.TradingRepository
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
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewOrderTab(
    modifier: Modifier = Modifier,
    isCompactOverlay: Boolean = false
) {
    val status by TradingRepository.connectionStatus.collectAsState()

    var symbol by remember { mutableStateOf("EURUSD") }
    var selectedOrderType by remember { mutableStateOf(OrderType.BUY) }
    var volume by remember { mutableDoubleStateOf(0.10) }
    var priceText by remember { mutableStateOf("") }
    var slPipsText by remember { mutableStateOf("20") }
    var tpPipsText by remember { mutableStateOf("40") }
    var lastExecutionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastExecutionMessage) {
        if (lastExecutionMessage != null) {
            delay(3500)
            lastExecutionMessage = null
        }
    }

    val isBuy = selectedOrderType == OrderType.BUY || selectedOrderType == OrderType.BUY_LIMIT || selectedOrderType == OrderType.BUY_STOP
    val isPending = selectedOrderType.isPending

    val popularSymbols = listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "BTCUSD", "US30")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactOverlay) 10.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Success / feedback banner
        item {
            AnimatedVisibility(visible = lastExecutionMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BuyGreenContainer)
                        .border(1.dp, BuyGreen, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BuyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = lastExecutionMessage ?: "",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Symbol Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRADING ASSET / SYMBOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = symbol.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularSymbols.forEach { sym ->
                            val isSelected = symbol.equals(sym, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BuyGreen else SlateCardElevated)
                                    .border(1.dp, if (isSelected) BuyGreen else SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable { symbol = sym }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("Custom Symbol", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("symbol_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BuyGreen,
                            unfocusedBorderColor = SlateBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        }

        // Order Type Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ORDER EXECUTION TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        letterSpacing = 1.sp
                    )

                    // Market Orders: BUY / SELL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBuySelected = selectedOrderType == OrderType.BUY
                        val isSellSelected = selectedOrderType == OrderType.SELL

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isBuySelected) BuyGreen else BuyGreenContainer.copy(alpha = 0.5f))
                                .border(1.dp, BuyGreen, RoundedCornerShape(10.dp))
                                .clickable { selectedOrderType = OrderType.BUY }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isBuySelected) Color.Black else BuyGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BUY (Market)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isBuySelected) Color.Black else BuyGreen
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSellSelected) SellRed else SellRedContainer.copy(alpha = 0.5f))
                                .border(1.dp, SellRed, RoundedCornerShape(10.dp))
                                .clickable { selectedOrderType = OrderType.SELL }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isSellSelected) Color.White else SellRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SELL (Market)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSellSelected) Color.White else SellRed
                                )
                            }
                        }
                    }

                    // Pending Orders
                    Text(
                        text = "Pending Orders:",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            OrderType.BUY_LIMIT,
                            OrderType.SELL_LIMIT,
                            OrderType.BUY_STOP,
                            OrderType.SELL_STOP
                        ).forEach { type ->
                            val isSelected = selectedOrderType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GoldAccent else SlateCardElevated)
                                    .border(1.dp, if (isSelected) GoldAccent else SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedOrderType = type }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }

                    if (isPending) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Pending Entry Price (0.0 for auto)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Volume / Lot Size Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ORDER VOLUME (LOTS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f Lots", volume),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = BuyGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (volume > 0.01) volume = ((volume - 0.10) * 100).toInt() / 100.0.coerceAtLeast(0.01) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateCardElevated)
                                .size(36.dp)
                        ) {
                            Text("-0.1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        IconButton(
                            onClick = { if (volume > 0.01) volume = ((volume - 0.01) * 100).toInt() / 100.0.coerceAtLeast(0.01) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateCardElevated)
                                .size(36.dp)
                        ) {
                            Text("-0.01", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateDark)
                                .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", volume),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = { volume = ((volume + 0.01) * 100).toInt() / 100.0.coerceAtMost(100.0) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateCardElevated)
                                .size(36.dp)
                        ) {
                            Text("+0.01", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        IconButton(
                            onClick = { volume = ((volume + 0.10) * 100).toInt() / 100.0.coerceAtMost(100.0) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateCardElevated)
                                .size(36.dp)
                        ) {
                            Text("+0.1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    // Lot presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.01, 0.05, 0.10, 0.25, 0.50, 1.00).forEach { lot ->
                            val isSelected = Math.abs(volume - lot) < 0.001
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) BuyGreen else SlateCardElevated)
                                    .clickable { volume = lot }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", lot),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // SL & TP in Pips Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val slNum = slPipsText.toDoubleOrNull() ?: 0.0
                    val tpNum = tpPipsText.toDoubleOrNull() ?: 0.0
                    val rrRatio = if (slNum > 0 && tpNum > 0) String.format(Locale.US, "1:%.1f", tpNum / slNum) else "--"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RISK MANAGEMENT (PIPS)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "R:R Ratio $rrRatio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stop Loss Field
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = slPipsText,
                                onValueChange = { slPipsText = it },
                                label = { Text("SL (Pips)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sl_pips_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SellRed,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(10, 20, 30, 50).forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SlateCardElevated)
                                            .clickable { slPipsText = p.toString() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("$p", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        // Take Profit Field
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = tpPipsText,
                                onValueChange = { tpPipsText = it },
                                label = { Text("TP (Pips)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("tp_pips_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BuyGreen,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(20, 40, 60, 100).forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SlateCardElevated)
                                            .clickable { tpPipsText = p.toString() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("$p", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Execution Button
        item {
            val isLive = status == ConnectionStatus.CONNECTED || status == ConnectionStatus.SIMULATED
            val buttonColor = if (isPending) GoldAccent else if (isBuy) BuyGreen else SellRed
            val textColor = if (isPending || isBuy) Color.Black else Color.White

            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val sl = slPipsText.toDoubleOrNull() ?: 0.0
                    val tp = tpPipsText.toDoubleOrNull() ?: 0.0

                    val success = TradingRepository.openOrder(
                        symbol = symbol,
                        type = selectedOrderType,
                        volume = volume,
                        price = price,
                        slPips = sl,
                        tpPips = tp
                    )

                    lastExecutionMessage = if (success) {
                        "Sent ${selectedOrderType.name} ${volume} Lots on ${symbol.uppercase()}"
                    } else {
                        "Failed to send: please check MetaTrader connection"
                    }
                },
                enabled = isLive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = SlateCardElevated
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("execute_order_button")
            ) {
                Text(
                    text = if (!isLive) {
                        "CONNECT TO EXECUTE"
                    } else {
                        "EXECUTE ${selectedOrderType.displayName.uppercase()} ${String.format(Locale.US, "%.2f", volume)} $symbol"
                    },
                    color = if (!isLive) TextMuted else textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
