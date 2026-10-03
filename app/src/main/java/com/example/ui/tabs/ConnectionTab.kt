package com.example.ui.tabs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.repository.TradingRepository
import com.example.ui.components.StatusBadge
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

@Composable
fun ConnectionTab(
    modifier: Modifier = Modifier,
    isCompactOverlay: Boolean = false
) {
    val status by TradingRepository.connectionStatus.collectAsState()
    val repoIp by TradingRepository.serverIp.collectAsState()
    val repoPort by TradingRepository.serverPort.collectAsState()
    val isSim by TradingRepository.isSimulationMode.collectAsState()
    val logs by TradingRepository.logs.collectAsState()

    var inputIp by remember(repoIp) { mutableStateOf(repoIp) }
    var inputPort by remember(repoPort) { mutableStateOf(repoPort.toString()) }

    val vpsPresets = listOf("185.120.45.60", "127.0.0.1", "localhost")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactOverlay) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // VPS Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "وضعیت اتصال به سرور مجازی (VPS)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (status) {
                                ConnectionStatus.CONNECTED -> "CONNECTED TO VPS (LIVE)"
                                ConnectionStatus.CONNECTING -> "CONNECTING TO VPS..."
                                ConnectionStatus.SIMULATED -> "SIMULATOR ACTIVE"
                                ConnectionStatus.LISTENING -> "STANDBY / READY FOR VPS"
                                else -> "OFFLINE / DISCONNECTED"
                            },
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isSim) "Internal Simulator Mode" else "Target: ws://$inputIp:$inputPort",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    StatusBadge(status = status)
                }
            }
        }

        // VPS IP Input Card
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = BuyGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تنظیمات آدرس سرور مجازی (VPS)",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "آدرس IP عمومی سرور مجازی خود (Public IP) را در کادر زیر وارد کنید:",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputIp,
                            onValueChange = {
                                inputIp = it
                                val port = inputPort.toIntOrNull() ?: 8080
                                TradingRepository.setServerConfig(it, port)
                            },
                            label = { Text("VPS Public IP / Domain", fontSize = 11.sp) },
                            placeholder = { Text("مثلاً 185.120.45.60", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isSim,
                            modifier = Modifier
                                .weight(2.2f)
                                .testTag("server_ip_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BuyGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lan,
                                    contentDescription = "IP",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        OutlinedTextField(
                            value = inputPort,
                            onValueChange = {
                                inputPort = it
                                val port = it.toIntOrNull() ?: 8080
                                TradingRepository.setServerConfig(inputIp, port)
                            },
                            label = { Text("Port", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isSim,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("server_port_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BuyGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Quick VPS Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "پریست‌های سریع:", fontSize = 10.sp, color = TextMuted)
                        vpsPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SlateCardElevated)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        inputIp = preset
                                        val port = inputPort.toIntOrNull() ?: 8080
                                        TradingRepository.setServerConfig(preset, port)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Connect / Disconnect Buttons
                    val isConnected = status == ConnectionStatus.CONNECTED

                    Button(
                        onClick = {
                            if (isConnected) {
                                TradingRepository.disconnect()
                            } else {
                                val port = inputPort.toIntOrNull() ?: 8080
                                TradingRepository.setServerConfig(inputIp, port)
                                TradingRepository.connect()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) SellRed else BuyGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("connect_disconnect_button")
                    ) {
                        Icon(
                            if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.CastConnected,
                            contentDescription = null,
                            tint = if (isConnected) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) "قطع ارتباط از VPS (DISCONNECT)" else "اتصال به متاتریدر سرور مجازی (CONNECT TO VPS)",
                            color = if (isConnected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Simulation Mode Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Sensors,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "شبیه‌ساز آفلاین معاملات (Interactive Test Mode)",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تست کامل دکمه‌ها و تغییر پیپ‌های SL/TP بدون نیاز به اتصال به VPS",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = isSim,
                        onCheckedChange = { TradingRepository.toggleSimulation(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldAccent,
                            checkedTrackColor = GoldAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SlateBorder
                        ),
                        modifier = Modifier.testTag("simulation_toggle")
                    )
                }
            }
        }

        // Live Log Terminal
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VPS COMMUNICATION LOGS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${logs.size} events",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactOverlay) 80.dp else 120.dp)
                    ) {
                        LazyColumn {
                            items(logs.take(15)) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    color = if (log.contains("fail", ignoreCase = true) || log.contains("error", ignoreCase = true)) {
                                        SellRed
                                    } else if (log.contains("pass", ignoreCase = true) || log.contains("connected", ignoreCase = true) || log.contains("live", ignoreCase = true)) {
                                        BuyGreen
                                    } else {
                                        TextSecondary
                                    },
                                    fontFamily = FontFamily.Monospace,
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
