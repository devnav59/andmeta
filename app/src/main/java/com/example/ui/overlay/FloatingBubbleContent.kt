package com.example.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.repository.TradingRepository
import com.example.ui.tabs.NewOrderTab
import com.example.ui.tabs.PositionsTab
import com.example.ui.theme.BuyGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SellRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.StatusConnecting
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun FloatingBubbleView(
    onClick: () -> Unit
) {
    val stats by TradingRepository.stats.collectAsState()
    val status by TradingRepository.connectionStatus.collectAsState()

    val isProfitable = stats.totalProfit >= 0
    val profitColor = if (isProfitable) BuyGreen else SellRed

    val statusDotColor = when (status) {
        ConnectionStatus.CONNECTED -> BuyGreen
        ConnectionStatus.LISTENING -> GoldAccent
        ConnectionStatus.SIMULATED -> GoldAccent
        ConnectionStatus.CONNECTING -> StatusConnecting
        ConnectionStatus.ERROR -> SellRed
        ConnectionStatus.DISCONNECTED -> StatusOffline
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(SlateDark)
            .border(2.dp, profitColor.copy(alpha = pulseAlpha), CircleShape)
            .clickable(onClick = onClick)
            .testTag("floating_bubble"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (stats.openCount > 0) {
                Text(
                    text = String.format(Locale.US, "%+,.0f", stats.totalProfit),
                    color = profitColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "${stats.openCount} Pos",
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "MT",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "READY",
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Compact, movable, and resizable Floating Control Panel
 */
@Composable
fun FloatingPanelWindow(
    onMinimize: () -> Unit,
    onCloseService: () -> Unit,
    onOpenFullApp: () -> Unit,
    onDragDelta: (Float, Float) -> Unit = { _, _ -> },
    onResizeDelta: (Float, Float) -> Unit = { _, _ -> },
    onToggleSizePreset: (Int) -> Unit = {},
    onToggleScannerTarget: () -> Unit = {}
) {
    val status by TradingRepository.connectionStatus.collectAsState()
    val stats by TradingRepository.stats.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = New Order, 1 = Positions
    var sizePreset by remember { mutableIntStateOf(1) } // 0 = S, 1 = M, 2 = L

    val statusDotColor = when (status) {
        ConnectionStatus.CONNECTED -> BuyGreen
        ConnectionStatus.LISTENING -> GoldAccent
        ConnectionStatus.SIMULATED -> GoldAccent
        ConnectionStatus.CONNECTING -> StatusConnecting
        ConnectionStatus.ERROR -> SellRed
        ConnectionStatus.DISCONNECTED -> StatusOffline
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(14.dp))
            .border(1.2.dp, SlateBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Unified Sleek Top Bar (Header + Inline Tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCardElevated)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Single Connection Indicator Light (+ Profit pill if active)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        if (stats.openCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%+,.0f", stats.totalProfit),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.totalProfit >= 0) BuyGreen else SellRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Center: Inline Compact Tabs (New Order & Positions)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 0: New Order
                        val isOrderTab = selectedTab == 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOrderTab) BuyGreen else SlateDark)
                                .clickable { selectedTab = 0 }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "اوردر جدید",
                                fontSize = 10.sp,
                                fontWeight = if (isOrderTab) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOrderTab) Color.Black else TextSecondary
                            )
                        }

                        // Tab 1: Positions
                        val isPosTab = selectedTab == 1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPosTab) BuyGreen else SlateDark)
                                .clickable { selectedTab = 1 }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "پوزیشن‌ها",
                                    fontSize = 10.sp,
                                    fontWeight = if (isPosTab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPosTab) Color.Black else TextSecondary
                                )
                                if (stats.openCount > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "(${stats.openCount})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPosTab) Color.Black else GoldAccent
                                    )
                                }
                            }
                        }
                    }

                    // Right: Target scanner, Size presets, Open full app, Minimize, Close
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle Floating Target Scanner Box
                        IconButton(
                            onClick = onToggleScannerTarget,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = "Scanner Target Box",
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        // Toggle Size Preset (S / M / L)
                        IconButton(
                            onClick = {
                                sizePreset = (sizePreset + 1) % 3
                                onToggleSizePreset(sizePreset)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.AspectRatio,
                                contentDescription = "Toggle Size",
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenFullApp,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = "Full App",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Minimize",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onCloseService,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SellRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Tab Content Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> NewOrderTab(isCompactOverlay = true)
                        else -> PositionsTab(isCompactOverlay = true)
                    }
                }
            }

            // Bottom-Right Corner Resize Grip Handle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResizeDelta(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val color = GoldAccent.copy(alpha = 0.6f)
                    val stroke = 1.5.dp.toPx()

                    // Diagonal lines grip
                    drawLine(color, Offset(w - 3.dp.toPx(), h - 11.dp.toPx()), Offset(w - 11.dp.toPx(), h - 3.dp.toPx()), stroke)
                    drawLine(color, Offset(w - 3.dp.toPx(), h - 7.dp.toPx()), Offset(w - 7.dp.toPx(), h - 3.dp.toPx()), stroke)
                    drawLine(color, Offset(w - 3.dp.toPx(), h - 3.dp.toPx()), Offset(w - 3.dp.toPx(), h - 3.dp.toPx()), stroke)
                }
            }
        }
    }
}
