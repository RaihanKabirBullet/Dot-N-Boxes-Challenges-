package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DotsDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = NavyVoid,
    primaryContainer = NavyCardElevated,
    onPrimaryContainer = AccentCyan,
    secondary = AccentCoral,
    onSecondary = NavyVoid,
    secondaryContainer = NavyCardElevated,
    onSecondaryContainer = AccentCoral,
    tertiary = AccentPurple,
    onTertiary = TextPrimary,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavyBase,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = NavyOutline,
    outlineVariant = NavyOutlineSubtle,
    error = ErrorRose,
    onError = TextPrimary,
)

private val DotsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun DotsAndBoxesTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DotsDarkColorScheme,
        typography = DotsTypography,
        shapes = DotsShapes,
        content = content,
    )
}
