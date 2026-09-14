package com.ericmbpeck.ai_sum.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.model.UsageWindow
import com.ericmbpeck.ai_sum.model.paceCopy
import com.ericmbpeck.ai_sum.ui.theme.BreachBar
import com.ericmbpeck.ai_sum.ui.theme.BreachFigure
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText

@Composable
fun WindowRow(
    window: UsageWindow,
    barColor: Color,
    figureColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    animationKey: Int = 0,
    dimmed: Boolean = false,
) {
    val breached = window.isBreached
    val fill = if (breached) BreachBar else barColor
    val figure = if (breached) BreachFigure else figureColor
    val paceRule = if (window.paceDelta > 12f) BreachBar else fill
    val barSegments = if (breached) {
        emptyList()
    } else {
        window.segments.map { segment ->
            BarSegment(weight = segment.percent, color = Color(segment.color.toInt()))
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (dimmed) 0.42f else 1f)
            .then(
                if (dimmed) {
                    Modifier
                } else {
                    Modifier.clickable(onClick = onToggle)
                },
            )
            .padding(bottom = 26.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = window.name,
                style = BroadsheetText.windowName,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${window.usagePercent.toInt()}%",
                style = BroadsheetText.usageFigure.copy(color = figure),
            )
        }
        UsageBar(
            usageFraction = window.usageFraction,
            elapsedFraction = window.elapsedFraction,
            barColor = fill,
            segments = barSegments,
            animationKey = animationKey,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = window.resetLabel, style = BroadsheetText.resetLabel)
            Text(text = window.elapsedLabel, style = BroadsheetText.resetLabel)
        }
        if (window.segments.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                window.segments.forEach { segment ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(Color(segment.color.toInt())),
                        )
                        Text(text = segment.label, style = BroadsheetText.legendLabel)
                        Text(
                            text = "${segment.percent.toInt()}%",
                            style = BroadsheetText.legendValue,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(visible = expanded) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .width(2.dp)
                        .height(36.dp)
                        .background(paceRule),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = paceCopy(window.paceDelta),
                    style = BroadsheetText.pace,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
