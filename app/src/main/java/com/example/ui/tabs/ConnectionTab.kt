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
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactOverlay) 10.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MetaTrader Bridge Status",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSim) "Internal Simulator (Port 8080)" else "ws://$inputIp:$inputPort",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    StatusBadge(status = status)
                }
            }
        }

        // Connection Form
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
                        text = "WebSocket Server Parameters",
                        fontSize = 13.sp,
                        color = GoldAccent,
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
                            label = { Text("PC / MT Server IP", fontSize = 11.sp) },
                            singleLine = true,
                            enabled = !isSim && status != ConnectionStatus.CONNECTED,
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
                                    modifier = Modifier.size(18.dp)
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
                            enabled = !isSim && status != ConnectionStatus.CONNECTED,
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

                    // Quick presets
                    if (!isCompactOverlay) {
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "192.168.1.100" to "Local Wi-Fi",
                                "10.0.2.2" to "Emulator Host",
                                "127.0.0.1" to "Localhost"
                            ).forEach { (ip, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateCardElevated)
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isSim && status != ConnectionStatus.CONNECTED) {
                                            inputIp = ip
                                            val port = inputPort.toIntOrNull() ?: 8080
                                            TradingRepository.setServerConfig(ip, port)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$ip ($label)",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Connect / Disconnect Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isConnectedOrSim = status == ConnectionStatus.CONNECTED || status == ConnectionStatus.SIMULATED

                        Button(
                            onClick = {
                                if (isConnectedOrSim) {
                                    TradingRepository.disconnect()
                                } else {
                                    val port = inputPort.toIntOrNull() ?: 8080
                                    TradingRepository.setServerConfig(inputIp, port)
                                    TradingRepository.connect()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnectedOrSim) SellRed else BuyGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("connect_disconnect_button")
                        ) {
                            Icon(
                                if (isConnectedOrSim) Icons.Default.PowerSettingsNew else Icons.Default.CastConnected,
                                contentDescription = null,
                                tint = if (isConnectedOrSim) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnectedOrSim) "DISCONNECT" else "CONNECT TO EA",
                                color = if (isConnectedOrSim) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
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
                        .padding(14.dp),
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
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تست لحظه‌ای اردرها، تغییر پیپ SL/TP و نوسان زنده سود بدون نیاز به متاتریدر روشن",
                            fontSize = 11.sp,
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
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TERMINAL LOGS",
                            fontSize = 11.sp,
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactOverlay) 90.dp else 130.dp)
                    ) {
                        LazyColumn {
                            items(logs.take(15)) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    color = if (log.contains("error", ignoreCase = true) || log.contains("failed", ignoreCase = true)) {
                                        SellRed
                                    } else if (log.contains("connected", ignoreCase = true) || log.contains("executed", ignoreCase = true)) {
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
