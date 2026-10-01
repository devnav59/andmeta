package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.repository.TradingRepository
import com.example.ui.tabs.ConnectionTab
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
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Circular Floating Bubble View rendered over all apps
 */
@Composable
fun FloatingBubbleView(
    onClick: () -> Unit
) {
    val status by TradingRepository.connectionStatus.collectAsState()
    val stats by TradingRepository.stats.collectAsState()

    val glowColor = when {
        status == ConnectionStatus.ERROR -> StatusError
        status == ConnectionStatus.CONNECTING -> StatusConnecting
        status == ConnectionStatus.DISCONNECTED -> StatusOffline
        stats.totalProfit > 0 -> BuyGreen
        stats.totalProfit < 0 -> SellRed
        else -> GoldAccent
    }

    val animatedGlow by animateColorAsState(targetValue = glowColor, label = "bubbleGlow")

    Box(
        modifier = Modifier
            .size(60.dp)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(SlateDark)
            .border(2.5.dp, animatedGlow, CircleShape)
            .clickable { onClick() }
            .testTag("floating_bubble_icon"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ShowChart,
                contentDescription = "MT Bubble",
                tint = animatedGlow,
                modifier = Modifier.size(24.dp)
            )

            if (stats.openCount > 0) {
                Text(
                    text = "${stats.openCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Live mini status dot
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedGlow)
                .border(1.5.dp, SlateDark, CircleShape)
        )
    }
}

/**
 * Expandable Floating Card Window containing the 3 tabs
 */
@Composable
fun FloatingPanelWindow(
    onMinimize: () -> Unit,
    onCloseService: () -> Unit,
    onOpenFullApp: () -> Unit
) {
    val status by TradingRepository.connectionStatus.collectAsState()
    val stats by TradingRepository.stats.collectAsState()

    var selectedTab by remember { mutableIntStateOf(2) } // default to Positions if any, else Connection

    val statusDotColor = when (status) {
        ConnectionStatus.CONNECTED -> BuyGreen
        ConnectionStatus.SIMULATED -> GoldAccent
        ConnectionStatus.CONNECTING -> StatusConnecting
        ConnectionStatus.ERROR -> SellRed
        ConnectionStatus.DISCONNECTED -> StatusOffline
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .border(1.5.dp, SlateBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCardElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MT Control Panel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (stats.openCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "(%+,.1f)", stats.totalProfit),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (stats.totalProfit >= 0) BuyGreen else SellRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenFullApp, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.OpenInFull,
                            contentDescription = "Open Full App",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onMinimize, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Minimize",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCloseService, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SellRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tab Navigation (3 Tabs specified in user prompt)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateCard,
                contentColor = BuyGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BuyGreen,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "اتصال",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) BuyGreen else TextSecondary
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "اردر جدید",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) BuyGreen else TextSecondary
                        )
                    },
                    icon = {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "پوزیشن‌ها",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) BuyGreen else TextSecondary
                            )
                            if (stats.openCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(BuyGreen)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "${stats.openCount}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    },
                    icon = {
                        Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ConnectionTab(isCompactOverlay = true)
                    1 -> NewOrderTab(isCompactOverlay = true)
                    2 -> PositionsTab(isCompactOverlay = true, onNavigateToNewOrder = { selectedTab = 1 })
                }
            }
        }
    }
}
