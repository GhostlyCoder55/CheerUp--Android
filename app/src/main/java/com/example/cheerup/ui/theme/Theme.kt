package com.example.cheerup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.cheerup.presentation.theme.CheerUpColors

private val CheerUpDarkScheme = darkColorScheme(
    primary = CheerUpColors.lightGrey,
    onPrimary = CheerUpColors.deepNavy,
    secondary = CheerUpColors.slate,
    onSecondary = CheerUpColors.lightGrey,
    tertiary = CheerUpColors.mutedSilver,
    background = CheerUpColors.deepNavy,
    onBackground = CheerUpColors.lightGrey,
    surface = CheerUpColors.darkBlue,
    onSurface = CheerUpColors.lightGrey,
    surfaceVariant = CheerUpColors.blueGrey,
    onSurfaceVariant = CheerUpColors.mutedSilver
)

@Composable
fun CheerUpTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CheerUpDarkScheme,
        typography = Typography,
        content = content
    )
}
