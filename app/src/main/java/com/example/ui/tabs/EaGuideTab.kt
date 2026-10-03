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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Security
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
    var selectedCodeTab by remember { mutableIntStateOf(0) } // 0 = PowerShell, 1 = MQL5, 2 = MQL4, 3 = Python, 4 = Bat
    var copiedCode by remember { mutableStateOf(false) }
    var copiedFirewall by remember { mutableStateOf(false) }

    val currentCode = when (selectedCodeTab) {
        0 -> MqlCodeProvider.powerShellBridgeCode
        1 -> MqlCodeProvider.mql5Code
        2 -> MqlCodeProvider.mql4Code
        3 -> MqlCodeProvider.pythonBridgeCode
        else -> MqlCodeProvider.batchScriptCode
    }

    val currentFileName = when (selectedCodeTab) {
        0 -> "bridge.ps1 (بدون نیاز به پایتون)"
        1 -> "MetaTrader_Bridge_EA.mq5"
        2 -> "MetaTrader_Bridge_EA.mq4"
        3 -> "bridge.py (پایتون)"
        else -> "start_bridge.bat"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Universal Firewall Command Card (Works in CMD and PowerShell)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
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
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دستور باز کردن پورت ۸۰۸۰ در فایروال VPS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Netsh Firewall Command", MqlCodeProvider.universalFirewallCommand)
                                clipboard.setPrimaryClip(clip)
                                copiedFirewall = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (copiedFirewall) BuyGreen else GoldAccent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                if (copiedFirewall) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedFirewall) "کپی شد!" else "کپی دستور ترمینال",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Text(
                        text = "در سرور مجازی، Command Prompt یا PowerShell را باز کرده (Run as Administrator) و این دستور را Paste و Enter کنید (بدون ارور کار می‌کند):",
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
                            text = MqlCodeProvider.universalFirewallCommand,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BuyGreen
                        )
                    }
                }
            }
        }

        // VPS Setup Step-by-Step Guide Card
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
                            text = "راهنمای اتصال متاتریدر سرور مجازی به موبایل",
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
                    // Scrollable Tab Selector for files
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
                            text = { Text("PowerShell (بدون پایتون)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                            text = { Text("bridge.py", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedCodeTab == 4,
                            onClick = { selectedCodeTab = 4; copiedCode = false },
                            text = { Text("start_bridge.bat", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
