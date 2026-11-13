package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EzbahTealLight,
    onPrimary = OnEzbahTealContainer,
    primaryContainer = EzbahTeal,
    onPrimaryContainer = EzbahTealContainer,
    secondary = EzbahGoldLight,
    onSecondary = OnEzbahGoldContainer,
    secondaryContainer = Color(0xFF4A310B),
    onSecondaryContainer = EzbahGoldContainer,
    tertiary = Color(0xFF93C5FD),
    onTertiary = OnEzbahNavyContainer,
    tertiaryContainer = EzbahNavy,
    onTertiaryContainer = EzbahNavyContainer,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = EzbahTeal,
    onPrimary = Color.White,
    primaryContainer = EzbahTealContainer,
    onPrimaryContainer = OnEzbahTealContainer,
    secondary = EzbahGold,
    onSecondary = Color.White,
    secondaryContainer = EzbahGoldContainer,
    onSecondaryContainer = OnEzbahGoldContainer,
    tertiary = EzbahNavy,
    onTertiary = Color.White,
    tertiaryContainer = EzbahNavyContainer,
    onTertiaryContainer = OnEzbahNavyContainer,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = LightOutline
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EzbahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
