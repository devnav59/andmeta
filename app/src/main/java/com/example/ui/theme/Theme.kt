package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BuyGreen,
    onPrimary = Color.Black,
    primaryContainer = BuyGreenContainer,
    onPrimaryContainer = BuyGreen,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldAccent,
    error = SellRed,
    onError = Color.White,
    errorContainer = SellRedContainer,
    onErrorContainer = SellRed,
    background = SlateDark,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = SlateCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Trading apps look best with the dark high-contrast scheme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
