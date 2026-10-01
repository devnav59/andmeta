package com.example.ui.tabs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.network.NetworkUtils
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

@OptIn(ExperimentalLayoutApi::class)
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

    val detectedIps = remember { NetworkUtils.getDeviceIpAddresses() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactOverlay) 8.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status Card
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
                        Text(
                            text = "MetaTrader Bridge Status",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (status) {
                                ConnectionStatus.CONNECTED -> "CONNECTED TO METATRADER"
                                ConnectionStatus.LISTENING -> "LISTENING ON 0.0.0.0:$repoPort"
                                ConnectionStatus.SIMULATED -> "SIMULATOR ACTIVE"
                                else -> "OFFLINE / DISCONNECTED"
                            },
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (status == ConnectionStatus.LISTENING) "آماده دریافت داده از Winlator (Port $repoPort)" else "Local Bridge Server Port $repoPort",
                            fontSize = 10.sp,
                            color = if (status == ConnectionStatus.LISTENING) GoldAccent else TextMuted
                        )
                    }
                    StatusBadge(status = status)
                }
            }
        }

        // Winlator Helper Card (Special for running MT on same mobile phone)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
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
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "راهنمای اتصال به Winlator (روی همین گوشی)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Button(
                            onClick = { TradingRepository.testPingInternalServer() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = BuyGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تست سرور داخلی", fontSize = 10.sp, color = BuyGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "سرور این اپلیکیشن روی پورت ۸۰۸۰ در پس‌زمینه همین گوشی فعال است. در متاتریدر Winlator در تنظیمات اکسپرت InpServerHost را روی یکی از این آی‌پی‌ها بگذارید:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        detectedIps.forEach { info ->
                            val isSelected = inputIp == info.ip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GoldAccent else SlateDark)
                                    .border(1.dp, if (isSelected) GoldAccent else SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        inputIp = info.ip
                                        val port = inputPort.toIntOrNull() ?: 8080
                                        TradingRepository.setServerConfig(info.ip, port)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Column {
                                    Text(
                                        text = info.ip,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = info.label,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Connection Form Card
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
                    Text(
                        text = "تنظیمات آدرس سرور (Server Address)",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
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
                            label = { Text("Server Host / IP", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isSim,
                            modifier = Modifier
                                .weight(2f)
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

                    // Connect / Disconnect Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                .weight(1f)
                                .height(42.dp)
                                .testTag("connect_disconnect_button")
                        ) {
                            Icon(
                                if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = if (isConnected) Color.White else Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) "DISCONNECT" else "START / REFRESH SERVER",
                                color = if (isConnected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
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
                                text = "شبیه‌ساز معاملات (Interactive Test Mode)",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تست لحظه‌ای اردرها، تغییر پیپ SL/TP و نوسان زنده سود بدون نیاز به متاتریدر روشن",
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
                            text = "COMMUNICATION LOGS",
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
                                    } else if (log.contains("pass", ignoreCase = true) || log.contains("connected", ignoreCase = true) || log.contains("running", ignoreCase = true)) {
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
