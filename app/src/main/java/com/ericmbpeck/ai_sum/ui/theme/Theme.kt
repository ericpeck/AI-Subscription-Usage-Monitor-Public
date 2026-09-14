package com.ericmbpeck.ai_sum.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BroadsheetColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    outline = Divider,
    error = BreachBar,
    onError = Paper,
    surfaceTint = Color.Transparent,
)

private val BroadsheetShapes = Shapes(
    extraSmall = RoundedCornerShape(1.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp),
)

@Composable
fun AISUMTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BroadsheetColorScheme,
        shapes = BroadsheetShapes,
        content = content,
    )
}
