package com.example.ui.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.BuyGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EaGuideTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCodeTab by remember { mutableIntStateOf(0) } // 0 = run_server.bat, 1 = MT5, 2 = MT4
    var copiedCode by remember { mutableStateOf(false) }
    var copiedInstantCmd by remember { mutableStateOf(false) }

    val currentCode = when (selectedCodeTab) {
        0 -> MqlCodeProvider.runServerBatCode
        1 -> MqlCodeProvider.mql5Code
        2 -> MqlCodeProvider.mql4Code
        3 -> MqlCodeProvider.pineScriptCode
        else -> MqlCodeProvider.tampermonkeyScriptCode
    }

    val currentFileName = when (selectedCodeTab) {
        0 -> "run_server.bat (بدون پایتون - هرگز بسته نمی‌شود)"
        1 -> "MetaTrader_Bridge_EA.mq5"
        2 -> "MetaTrader_Bridge_EA.mq4"
        3 -> "tradingview_strategy_with_signal_row.pine"
        else -> "tradingview_watcher.user.js (اتوتریدر وب)"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fast 1-Click Instant Command Card (No file creation needed!)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, BuyGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "روش فوق‌سریع: دستور ۱-خطی (بدون ساخت هیچ فایلی!)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("1-Line Server Command", MqlCodeProvider.oneLineVpsCommand)
                                clipboard.setPrimaryClip(clip)
                                copiedInstantCmd = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copiedInstantCmd) BuyGreen else GoldAccent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                if (copiedInstantCmd) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedInstantCmd) "کپی شد!" else "کپی دستور ۱-خطی",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Text(
                        text = "کافیست در سرور مجازی، PowerShell یا CMD را باز کرده و این دستور را Paste و Enter کنید. سرور فوراً فعال شده و پنجره باز می‌ماند:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateDark)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = MqlCodeProvider.oneLineVpsCommand.take(160) + " ...",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BuyGreen
                        )
                    }
                }
            }
        }

        // Setup Guide Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "راهنمای حل مشکل بسته شدن پنجره در سرور مجازی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = MqlCodeProvider.guideTextFa,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Code Viewer Card
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
                    // Scrollable Tab Selector
                    ScrollableTabRow(
                        selectedTabIndex = selectedCodeTab,
                        containerColor = SlateCardElevated,
                        contentColor = BuyGreen,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedCodeTab]),
                                color = BuyGreen,
                                height = 2.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedCodeTab == 0,
                            onClick = { selectedCodeTab = 0; copiedCode = false },
                            text = { Text("run_server.bat (بدون بسته شدن)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCodeTab == 1,
                            onClick = { selectedCodeTab = 1; copiedCode = false },
                            text = { Text("MT5 (MQL5)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCodeTab == 2,
                            onClick = { selectedCodeTab = 2; copiedCode = false },
                            text = { Text("MT4 (MQL4)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCodeTab == 3,
                            onClick = { selectedCodeTab = 3; copiedCode = false },
                            text = { Text("اندیکاتور تریدینگ‌ویو (Pine)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCodeTab == 4,
                            onClick = { selectedCodeTab = 4; copiedCode = false },
                            text = { Text("اسکریپت تمپرمانکی وب", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentFileName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(currentFileName, currentCode)
                                clipboard.setPrimaryClip(clip)
                                copiedCode = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copiedCode) BuyGreen else SlateCardElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                if (copiedCode) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (copiedCode) Color.Black else TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedCode) "کپی شد!" else "COPY CODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (copiedCode) Color.Black else TextPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateDark)
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = currentCode,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
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
