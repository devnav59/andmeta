package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mql.MqlCodeProvider
import com.example.repository.TradingRepository
import com.example.service.ScreenScannerManager
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
import java.util.Locale

@Composable
fun AutoTraderTab(
    modifier: Modifier = Modifier,
    onRequestScreenCapture: () -> Unit = {},
    onToggleScannerBox: () -> Unit = {}
) {
    val context = LocalContext.current
    val isAutoTraderEnabled by TradingRepository.isAutoTraderEnabled.collectAsState()
    val isNotificationActive by TradingRepository.isNotificationListenerActive.collectAsState()
    val autoLot by TradingRepository.autoTradeLot.collectAsState()
    val autoSl by TradingRepository.autoTradeSlPips.collectAsState()
    val autoTp by TradingRepository.autoTradeTpPips.collectAsState()
    val logs by TradingRepository.autoTradeLogs.collectAsState()

    val hasProjection by ScreenScannerManager.hasProjectionPermission.collectAsState()
    val isScanning by ScreenScannerManager.isScannerActive.collectAsState()
    val isTargetBoxVisible by ScreenScannerManager.isTargetBoxVisible.collectAsState()
    val detectedSignal by ScreenScannerManager.detectedSignal.collectAsState()
    val detectedColor by ScreenScannerManager.detectedColor.collectAsState()

    var copiedPine by remember { mutableStateOf(false) }
    var copiedScript by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Enable / Disable Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoTraderEnabled) BuyGreen.copy(alpha = 0.12f) else SlateCardElevated
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isAutoTraderEnabled) BuyGreen else SlateBorder,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isAutoTraderEnabled) BuyGreen else SlateDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isAutoTraderEnabled) Color.Black else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "اتوتریدر خودکار تریدینگ‌ویو",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isAutoTraderEnabled) "🟢 فعال و آماده باز/بستن خودکار" else "⚪ غیرفعال (استندبای)",
                                fontSize = 11.sp,
                                color = if (isAutoTraderEnabled) BuyGreen else TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isAutoTraderEnabled,
                        onCheckedChange = { TradingRepository.setAutoTraderEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BuyGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SlateDark
                        )
                    )
                }
            }
        }

        // Floating Target Scanner Card (Requested by user: Screen Scanner Box)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (hasProjection) BuyGreen else SellRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎯 مربع شناور اسکنر سیگنال (Screen Scanner)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }

                        // Live detected signal badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SlateDark)
                                .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(detectedColor)
                                    .border(0.5.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = detectedSignal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = when (detectedSignal) {
                                    "BUY" -> BuyGreen
                                    "SELL" -> SellRed
                                    "CLOSE" -> GoldAccent
                                    else -> TextSecondary
                                },
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "مربع شناور را روی کادر سطر SIGNAL در جدول چارت تریدینگ‌ویو قرار دهید. اپلیکیشن با اسکن رنگ کادر (سبز = BUY، قرمز = SELL، نارنجی = CLOSE)، پوزیشن‌ها را بدون نیاز به هیچ الرت یا اشتراک پولی باز و بسته می‌کند.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    // Control Buttons for Target Scanner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Grant Screen Capture Permission
                        Button(
                            onClick = onRequestScreenCapture,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasProjection) SlateDark else BuyGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = if (hasProjection) "مجوز اسکن فعال است" else "درخواست مجوز اسکن",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasProjection) BuyGreen else Color.Black
                            )
                        }

                        // 2. Toggle Floating Target Box on Screen
                        Button(
                            onClick = onToggleScannerBox,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(
                                text = "نمایش / جابجایی مربع هدف",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Notification Permission Card (For Free TradingView mobile alerts)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دریافت خودکار از نوتیفیکیشن (رایگان)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تنظیم دسترسی",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Text(
                        text = "در نسخه رایگان تریدینگ‌ویو، هنگام ساخت الرت، گزینه «Notify in app» را تیک بزنید. با هر پیام، این اپلیکیشن بدون نیاز به وب‌هوک پولی، معامله را در متاتریدر ثبت می‌کند.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Auto-Trade Order Settings (Lot, SL, TP)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تنظیمات حجم و حد سود/ضرر اتوتریدر",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Lot Size selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حجم ورود (Lot):",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0.01, 0.02, 0.05, 0.10).forEach { lot ->
                                val isSelected = autoLot == lot
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) BuyGreen else SlateDark)
                                        .border(1.dp, if (isSelected) BuyGreen else SlateBorder, RoundedCornerShape(6.dp))
                                        .clickable { TradingRepository.setAutoTradeLot(lot) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$lot",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Test Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { TradingRepository.processAutoTradeSignal("BUY", "دکمه تست دستی") },
                            colors = ButtonDefaults.buttonColors(containerColor = BuyGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("تست BUY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = { TradingRepository.processAutoTradeSignal("SELL", "دکمه تست دستی") },
                            colors = ButtonDefaults.buttonColors(containerColor = SellRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("تست SELL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { TradingRepository.processAutoTradeSignal("CLOSE", "دکمه تست دستی") },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCardElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Text("تست CLOSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Live Auto-Trade Logs
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
                        text = "گزارش زنده رویدادهای اتوتریدر",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateDark)
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            items(logs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        log.contains("BUY") -> BuyGreen
                                        log.contains("SELL") -> SellRed
                                        log.contains("ACTION") -> GoldAccent
                                        else -> TextSecondary
                                    },
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
