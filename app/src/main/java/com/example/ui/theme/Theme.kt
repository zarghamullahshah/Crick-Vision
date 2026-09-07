package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NaturalColorScheme = lightColorScheme(
    primary = NaturalSage,
    onPrimary = Color.White,
    primaryContainer = NaturalSageLight,
    onPrimaryContainer = NaturalSage,
    secondary = NaturalSlateText,
    onSecondary = Color.White,
    secondaryContainer = NaturalSand,
    onSecondaryContainer = NaturalTextPrimary,
    tertiary = NaturalOchre,
    onTertiary = Color.White,
    background = NaturalBackground,
    surface = NaturalCardWhite,
    onBackground = NaturalTextPrimary,
    onSurface = NaturalTextPrimary,
    surfaceVariant = NaturalSurface,
    onSurfaceVariant = NaturalTextSecondary,
    outline = NaturalBorderSubtle,
    outlineVariant = NaturalSandBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NaturalColorScheme,
        typography = Typography,
        content = content
    )
}

