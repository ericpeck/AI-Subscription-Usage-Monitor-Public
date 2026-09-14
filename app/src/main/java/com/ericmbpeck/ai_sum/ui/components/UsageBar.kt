package com.ericmbpeck.ai_sum.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.BreachBar
import com.ericmbpeck.ai_sum.ui.theme.CursorBar
import com.ericmbpeck.ai_sum.ui.theme.GrokSegmentChat
import com.ericmbpeck.ai_sum.ui.theme.GrokSegmentCoding
import com.ericmbpeck.ai_sum.ui.theme.GrokSegmentVoice
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral200

data class BarSegment(
    val weight: Float,
    val color: Color,
)

@Composable
fun UsageBar(
    usageFraction: Float,
    elapsedFraction: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
    segments: List<BarSegment> = emptyList(),
    animationKey: Int = 0,
) {
    val usage = usageFraction.coerceIn(0f, 1f)
    val elapsed = elapsedFraction.coerceIn(0f, 1f)
    val animatable = remember { Animatable(usage) }
    LaunchedEffect(usage, animationKey) {
        if (animationKey > 0) {
            animatable.snapTo(0f)
        }
        animatable.animateTo(usage, tween(durationMillis = 350))
    }
    val hairlineWidth = with(LocalDensity.current) { 1.dp.toPx() }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val trackTop = 5.dp.toPx()
        val trackHeight = 10.dp.toPx()
        val fillWidth = size.width * animatable.value
        val gap = 1.dp.toPx()
        drawRect(
            color = Neutral200,
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
        )
        if (segments.isEmpty()) {
            drawRect(
                color = barColor,
                topLeft = Offset(0f, trackTop),
                size = Size(fillWidth, trackHeight),
            )
        } else {
            val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
            val inner = (fillWidth - gap * (segments.size - 1)).coerceAtLeast(0f)
            var x = 0f
            segments.forEach { segment ->
                val width = inner * (segment.weight / totalWeight)
                if (width > 0f) {
                    drawRect(
                        color = segment.color,
                        topLeft = Offset(x, trackTop),
                        size = Size(width, trackHeight),
                    )
                }
                x += width + gap
            }
        }
        val hairlineX = (size.width * elapsed).coerceIn(0f, size.width)
        drawRect(
            color = Ink,
            topLeft = Offset(hairlineX - hairlineWidth / 2f, 0f),
            size = Size(hairlineWidth, size.height),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarEmptyPreview() {
    AISUMTheme { UsageBar(usageFraction = 0f, elapsedFraction = 0.1f, barColor = CursorBar) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarMidPreview() {
    AISUMTheme { UsageBar(usageFraction = 0.45f, elapsedFraction = 0.10f, barColor = CursorBar) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarBreachedPreview() {
    AISUMTheme { UsageBar(usageFraction = 0.94f, elapsedFraction = 0.96f, barColor = BreachBar) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarBehindClockPreview() {
    AISUMTheme { UsageBar(usageFraction = 0.31f, elapsedFraction = 0.67f, barColor = CursorBar) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarAheadOfClockPreview() {
    AISUMTheme { UsageBar(usageFraction = 0.58f, elapsedFraction = 0.44f, barColor = CursorBar) }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun UsageBarSegmentedPreview() {
    AISUMTheme {
        UsageBar(
            usageFraction = 0.44f,
            elapsedFraction = 0.86f,
            barColor = GrokSegmentVoice,
            segments = listOf(
                BarSegment(19f, GrokSegmentVoice),
                BarSegment(5f, GrokSegmentChat),
                BarSegment(20f, GrokSegmentCoding),
            ),
        )
    }
}
