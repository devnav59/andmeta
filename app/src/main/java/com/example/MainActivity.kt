package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.repository.TradingRepository
import com.example.service.FloatingBubbleService
import com.example.ui.components.StatusBadge
import com.example.ui.tabs.ConnectionTab
import com.example.ui.tabs.EaGuideTab
import com.example.ui.tabs.NewOrderTab
import com.example.ui.tabs.PositionsTab
import com.example.ui.theme.BuyGreen
import com.example.ui.theme.BuyGreenContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SellRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val hasOverlayPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var hasOverlayPermission by remember { hasOverlayPermissionState }
                val isServiceRunning by TradingRepository.isServiceRunning.collectAsState()
                val status by TradingRepository.connectionStatus.collectAsState()
                val stats by TradingRepository.stats.collectAsState()
                var selectedScreen by remember { mutableIntStateOf(0) }

                // Post notification permission launcher
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Result handled */ }

                val overlayPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    hasOverlayPermission = Settings.canDrawOverlays(this)
                }

                val mediaProjectionManager = remember {
                    context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                }

                val screenCaptureLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                        val dm = context.resources.displayMetrics
                        com.example.service.ScreenScannerManager.initProjection(
                            context = context,
                            resultCode = result.resultCode,
                            data = result.data!!,
                            width = dm.widthPixels,
                            height = dm.heightPixels,
                            dpi = dm.densityDpi
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    hasOverlayPermission = Settings.canDrawOverlays(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SlateDark,
                    bottomBar = {
                        NavigationBar(
                            containerColor = SlateCard,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedScreen == 0,
                                onClick = { selectedScreen = 0 },
                                icon = { Icon(Icons.Default.Lan, contentDescription = "Connection") },
                                label = { Text("اتصال", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = BuyGreen,
                                    indicatorColor = BuyGreen,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedScreen == 1,
                                onClick = { selectedScreen = 1 },
                                icon = { Icon(Icons.Default.TrendingUp, contentDescription = "New Order") },
                                label = { Text("اردر جدید", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = BuyGreen,
                                    indicatorColor = BuyGreen,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedScreen == 2,
                                onClick = { selectedScreen = 2 },
                                icon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrackChanges, contentDescription = "Positions")
                                        if (stats.openCount > 0) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(BuyGreen)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    "${stats.openCount}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                },
                                label = { Text("پوزیشن‌ها", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = BuyGreen,
                                    indicatorColor = BuyGreen,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedScreen == 3,
                                onClick = { selectedScreen = 3 },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = "EA Guide") },
                                label = { Text("اکسپرت MQL", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = GoldAccent,
                                    indicatorColor = GoldAccent,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedScreen == 4,
                                onClick = { selectedScreen = 4 },
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Auto Trader") },
                                label = { Text("اتوتریدر", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = GoldAccent,
                                    indicatorColor = GoldAccent,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Top Header Bar
                        TopAppHeader(
                            status = status,
                            hasOverlayPermission = hasOverlayPermission,
                            isServiceRunning = isServiceRunning,
                            onToggleFloatingBubble = {
                                if (!hasOverlayPermission) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                } else {
                                    if (isServiceRunning) {
                                        FloatingBubbleService.stop(context)
                                    } else {
                                        FloatingBubbleService.start(context)
                                    }
                                }
                            }
                        )

                        // Permission alert card if not yet granted
                        AnimatedVisibility(visible = !hasOverlayPermission) {
                            OverlayPermissionBanner(
                                onRequestPermission = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlayPermissionLauncher.launch(intent)
                                }
                            )
                        }

                        // Main Content Tabs
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedScreen) {
                                0 -> ConnectionTab()
                                1 -> NewOrderTab()
                                2 -> PositionsTab(onNavigateToNewOrder = { selectedScreen = 1 })
                                3 -> EaGuideTab()
                                4 -> com.example.ui.tabs.AutoTraderTab(
                                    onRequestScreenCapture = {
                                        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                    },
                                    onToggleScannerBox = {
                                        com.example.service.FloatingBubbleService.toggleScanner(context)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermissionState.value = Settings.canDrawOverlays(this)
    }
}

@Composable
fun TopAppHeader(
    status: com.example.model.ConnectionStatus,
    hasOverlayPermission: Boolean,
    isServiceRunning: Boolean,
    onToggleFloatingBubble: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(BuyGreenContainer)
                        .border(1.5.dp, BuyGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BubbleChart,
                        contentDescription = "Logo",
                        tint = BuyGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "MetaTrader Bubble",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "MQL Bridge Controller",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Launch Floating Bubble Button
            Button(
                onClick = onToggleFloatingBubble,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) SellRed else BuyGreen
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("floating_bubble_toggle_button")
            ) {
                Icon(
                    if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isServiceRunning) Color.White else Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (!hasOverlayPermission) "GRANT OVERLAY" else if (isServiceRunning) "STOP BUBBLE" else "LAUNCH BUBBLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isServiceRunning) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun OverlayPermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "دسترسی دکمه شناور روی صفحه",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "برای نمایش کنترل پنل متاتریدر روی بقیه برنامه‌ها، دسترسی Overlay لازم است.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "فعال‌سازی",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
