package com.example.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ScreenScannerManager
import com.example.ui.theme.BuyGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SellRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun FloatingScannerTargetBox(
    onClose: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onResizeDelta: (Float, Float) -> Unit
) {
    val signal by ScreenScannerManager.detectedSignal.collectAsState()
    val detectedColor by ScreenScannerManager.detectedColor.collectAsState()
    val isScanning by ScreenScannerManager.isScannerActive.collectAsState()

    val borderColor by animateColorAsState(
        targetValue = when (signal) {
            "BUY" -> BuyGreen
            "SELL" -> SellRed
            "CLOSE" -> GoldAccent
            else -> if (isScanning) GoldAccent.copy(alpha = 0.8f) else SlateBorder
        },
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(2.dp, borderColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Mini Header & Drag Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCardElevated.copy(alpha = 0.85f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragDelta(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(detectedColor)
                                .border(1.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (signal) {
                                "BUY" -> "🟢 BUY"
                                "SELL" -> "🔴 SELL"
                                "CLOSE" -> "🟠 CLOSE"
                                "FLAT" -> "⚪ FLAT"
                                else -> "🎯 اسکن..."
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = borderColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SellRed,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Center Transparent Target Area with Reticle Crosshairs
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f
                        val cy = h / 2f
                        val crossSize = 10.dp.toPx()

                        // Center crosshairs
                        drawLine(
                            borderColor.copy(alpha = 0.6f),
                            Offset(cx - crossSize, cy),
                            Offset(cx + crossSize, cy),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            borderColor.copy(alpha = 0.6f),
                            Offset(cx, cy - crossSize),
                            Offset(cx, cy + crossSize),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Corner Reticles
                        val cornerLen = 8.dp.toPx()
                        val stroke = 2.dp.toPx()
                        // Top-Left
                        drawLine(borderColor, Offset(4f, 4f), Offset(4f + cornerLen, 4f), stroke)
                        drawLine(borderColor, Offset(4f, 4f), Offset(4f, 4f + cornerLen), stroke)
                        // Top-Right
                        drawLine(borderColor, Offset(w - 4f, 4f), Offset(w - 4f - cornerLen, 4f), stroke)
                        drawLine(borderColor, Offset(w - 4f, 4f), Offset(w - 4f, 4f + cornerLen), stroke)
                        // Bottom-Left
                        drawLine(borderColor, Offset(4f, h - 4f), Offset(4f + cornerLen, h - 4f), stroke)
                        drawLine(borderColor, Offset(4f, h - 4f), Offset(4f, h - 4f - cornerLen), stroke)
                    }
                }
            }

            // Bottom-Right Corner Resize Grip Handle
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onResizeDelta(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val w = size.width
                    val h = size.height
                    val color = borderColor.copy(alpha = 0.7f)
                    val stroke = 1.5.dp.toPx()
                    drawLine(color, Offset(w - 2.dp.toPx(), h - 8.dp.toPx()), Offset(w - 8.dp.toPx(), h - 2.dp.toPx()), stroke)
                    drawLine(color, Offset(w - 2.dp.toPx(), h - 4.dp.toPx()), Offset(w - 4.dp.toPx(), h - 2.dp.toPx()), stroke)
                }
            }
        }
    }
}
