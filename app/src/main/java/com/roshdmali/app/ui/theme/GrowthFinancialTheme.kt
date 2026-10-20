package com.roshdmali.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = WarmBackground,
    secondary = WarmGold,
    background = WarmBackground,
    onBackground = DeepInk,
    surface = WarmBackground,
    onSurface = DeepInk,
    surfaceVariant = WarmBackground,
    onSurfaceVariant = MutedInk,
)

@Composable
fun GrowthFinancialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
