package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SellRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun NewOrderTab(
    modifier: Modifier = Modifier,
    isCompactOverlay: Boolean = false
) {
    val status by TradingRepository.connectionStatus.collectAsState()

    var volume by remember { mutableDoubleStateOf(0.01) }
    var volumeText by remember { mutableStateOf("0.01") }

    var isSlTpEnabled by remember { mutableStateOf(false) }
    var slPipsText by remember { mutableStateOf("20") }
    var tpPipsText by remember { mutableStateOf("40") }
    var activePipField by remember { mutableStateOf("SL") } // "SL" or "TP"

    var isPendingExpanded by remember { mutableStateOf(false) }
    var pendingType by remember { mutableStateOf(OrderType.BUY_LIMIT) }
    var pendingPriceText by remember { mutableStateOf("") }

    var lastExecutionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastExecutionMessage) {
        if (lastExecutionMessage != null) {
            delay(3000)
            lastExecutionMessage = null
        }
    }

    val microLots = listOf(0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.07, 0.08, 0.09)
    val miniLots = listOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)
    val pipPresets = listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    fun executeOrder(type: OrderType, price: Double = 0.0) {
        val slPips = if (isSlTpEnabled) slPipsText.toDoubleOrNull() ?: 0.0 else 0.0
        val tpPips = if (isSlTpEnabled) tpPipsText.toDoubleOrNull() ?: 0.0 else 0.0
        val lot = volumeText.toDoubleOrNull() ?: volume

        TradingRepository.openOrder(
            symbol = "", // MetaTrader EA uses _Symbol of the active chart!
            type = type,
            volume = lot,
            price = price,
            slPips = slPips,
            tpPips = tpPips
        )
        lastExecutionMessage = "${type.name} $lot Lot (چارت فعال)"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (isCompactOverlay) 6.dp else 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Chart Notice & Feedback
        if (lastExecutionMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BuyGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BuyGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت شد: ${lastExecutionMessage.orEmpty()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BuyGreen
                        )
                    }
                }
            }
        }

        // 1. Primary BUY & SELL Execution Row with Volume in between
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BUY Button
                    Button(
                        onClick = { executeOrder(OrderType.BUY) },
                        colors = ButtonDefaults.buttonColors(containerColor = BuyGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("floating_buy_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BUY", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                            Text("خرید سریع", fontSize = 9.sp, color = Color.Black.copy(alpha = 0.8f))
                        }
                    }

                    // Volume (Lot) Input in the Middle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateDark)
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حجم (Lot)", fontSize = 9.sp, color = TextMuted)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        val cur = volumeText.toDoubleOrNull() ?: volume
                                        val next = (cur - 0.01).coerceAtLeast(0.01)
                                        volume = next
                                        volumeText = String.format(Locale.US, "%.2f", next)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }

                                OutlinedTextField(
                                    value = volumeText,
                                    onValueChange = {
                                        volumeText = it
                                        it.toDoubleOrNull()?.let { v -> volume = v }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = GoldAccent
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.width(64.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val cur = volumeText.toDoubleOrNull() ?: volume
                                        val next = (cur + 0.01)
                                        volume = next
                                        volumeText = String.format(Locale.US, "%.2f", next)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // SELL Button
                    Button(
                        onClick = { executeOrder(OrderType.SELL) },
                        colors = ButtonDefaults.buttonColors(containerColor = SellRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("floating_sell_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SELL", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Text("فروش سریع", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        // 2. Quick Lot Selection: Two Rows (0.01 to 0.09 and 0.1 to 0.9)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: 0.01 to 0.09
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        microLots.forEach { lot ->
                            val isSelected = (volumeText.toDoubleOrNull() ?: volume) == lot
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) BuyGreen else SlateDark)
                                    .border(1.dp, if (isSelected) BuyGreen else SlateBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        volume = lot
                                        volumeText = String.format(Locale.US, "%.2f", lot)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f", lot),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Row 2: 0.1 to 0.9
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        miniLots.forEach { lot ->
                            val isSelected = (volumeText.toDoubleOrNull() ?: volume) == lot
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) GoldAccent else SlateDark)
                                    .border(1.dp, if (isSelected) GoldAccent else SlateBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        volume = lot
                                        volumeText = String.format(Locale.US, "%.1f", lot)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", lot),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SL / TP Checkbox & Inputs with 10 to 100 Pip quick buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSlTpEnabled = !isSlTpEnabled },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSlTpEnabled,
                            onCheckedChange = { isSlTpEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GoldAccent,
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعیین حد ضرر و سود (SL / TP به پیپ)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSlTpEnabled) GoldAccent else TextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (isSlTpEnabled) "فعال" else "بدون استاپ/تیپی",
                            fontSize = 9.sp,
                            color = if (isSlTpEnabled) BuyGreen else TextMuted
                        )
                    }

                    // SL & TP Inputs side by side
                    AnimatedVisibility(visible = isSlTpEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Stop Loss Field
                                val isSlActive = activePipField == "SL"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateDark)
                                        .border(
                                            1.5.dp,
                                            if (isSlActive) SellRed else SlateBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activePipField = "SL" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("حد ضرر (SL)", fontSize = 9.sp, color = SellRed, fontWeight = FontWeight.Bold)
                                            if (isSlActive) {
                                                Text("انتخاب شده", fontSize = 8.sp, color = SellRed)
                                            }
                                        }
                                        OutlinedTextField(
                                            value = slPipsText,
                                            onValueChange = { slPipsText = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Take Profit Field
                                val isTpActive = activePipField == "TP"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateDark)
                                        .border(
                                            1.5.dp,
                                            if (isTpActive) BuyGreen else SlateBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { activePipField = "TP" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("حد سود (TP)", fontSize = 9.sp, color = BuyGreen, fontWeight = FontWeight.Bold)
                                            if (isTpActive) {
                                                Text("انتخاب شده", fontSize = 8.sp, color = BuyGreen)
                                            }
                                        }
                                        OutlinedTextField(
                                            value = tpPipsText,
                                            onValueChange = { tpPipsText = it },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // 10 to 100 Pips Quick Row
                            Text(
                                text = "کلیک روی پیپ برای درج در فیلد ${if (activePipField == "SL") "حد ضرر (SL)" else "حد سود (TP)"}:",
                                fontSize = 9.sp,
                                color = TextMuted
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                pipPresets.forEach { pip ->
                                    val isCurrentValue = (if (activePipField == "SL") slPipsText else tpPipsText) == pip.toString()
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isCurrentValue) {
                                                    if (activePipField == "SL") SellRed else BuyGreen
                                                } else SlateDark
                                            )
                                            .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                if (activePipField == "SL") {
                                                    slPipsText = pip.toString()
                                                } else {
                                                    tpPipsText = pip.toString()
                                                }
                                            }
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$pip",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrentValue) Color.White else TextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Pending Orders Structure
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPendingExpanded = !isPendingExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اردرهای شرطی (Pending Orders)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = { isPendingExpanded = !isPendingExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (isPendingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    }

                    AnimatedVisibility(visible = isPendingExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 4 Types selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    OrderType.BUY_LIMIT,
                                    OrderType.SELL_LIMIT,
                                    OrderType.BUY_STOP,
                                    OrderType.SELL_STOP
                                ).forEach { type ->
                                    val isSelected = pendingType == type
                                    val isBuyType = type.name.startsWith("BUY")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) {
                                                    if (isBuyType) BuyGreen else SellRed
                                                } else SlateDark
                                            )
                                            .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                            .clickable { pendingType = type }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.name.replace("_", " "),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) (if (isBuyType) Color.Black else Color.White) else TextSecondary
                                        )
                                    }
                                }
                            }

                            // Price input
                            OutlinedTextField(
                                value = pendingPriceText,
                                onValueChange = { pendingPriceText = it },
                                label = { Text("قیمت شرطی (اختیاری - پیش‌فرض ۲۰ پیپ فاصله)", fontSize = 10.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BuyGreen,
                                    unfocusedBorderColor = SlateBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            // Submit Pending Button
                            Button(
                                onClick = {
                                    val targetPrice = pendingPriceText.toDoubleOrNull() ?: 0.0
                                    executeOrder(pendingType, targetPrice)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pendingType.name.startsWith("BUY")) BuyGreen else SellRed
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = "ثبت اردر ${pendingType.name.replace("_", " ")}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingType.name.startsWith("BUY")) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
