package com.ericmbpeck.ai_sum.watch

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.res.ResourcesCompat
import androidx.wear.compose.material3.Text
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.wearbridge.WatchArcMath
import com.ericmbpeck.ai_sum.wearbridge.WatchBoardScale
import com.ericmbpeck.ai_sum.wearbridge.WatchLabelClearance
import com.ericmbpeck.ai_sum.wearbridge.WearAccount
import com.ericmbpeck.ai_sum.wearbridge.WearResetCopy
import com.ericmbpeck.ai_sum.wearbridge.WearWindow
import kotlin.math.cos
import kotlin.math.sin

private const val BREACH = 90f

@Composable
fun UsageArcFace(
    account: WearAccount,
    modifier: Modifier = Modifier,
) {
    val scale = LocalWatchScale.current
    val inks = WatchInks.of(account.platformId)
    val (outer, inner) = WatchArcMath.splitWindows(account.windows)
    val centre = WatchArcMath.centreWindow(account)
    val grokBot = account.platformId == "grokbot"
    val grokParts = outer?.segments.orEmpty().isNotEmpty() && inner == null
    val work = account.kind == "WORK"
    val outerBreached = (outer?.usagePercent ?: 0f) >= BREACH
    val outerColor = when {
        account.platformId == "cursor" && outerBreached -> WatchInks.cursorBreach
        else -> inks.outer
    }
    val figureColor = when {
        account.platformId == "cursor" && outerBreached -> WatchInks.cursorBreachFigure
        grokParts -> inks.figure
        inner != null -> inks.inner
        else -> inks.figure
    }
    val nameSwatch = when {
        account.platformId == "grok" -> WatchInks.tick
        account.platformId == "cursor" && outerBreached -> WatchInks.cursorBreach
        else -> outerColor
    }
    val context = LocalContext.current
    val typeface = remember {
        ResourcesCompat.getFont(context, R.font.source_serif_4_semibold) ?: Typeface.DEFAULT_BOLD
    }
    val density = LocalDensity.current
    val resetTopPadding = with(density) {
        (
            scale.screenMinPx / 2f +
                WatchLabelClearance.resetSlotTopFromCenterPx(scale.screenMinPx)
            ).toDp()
    }
    Box(modifier.fillMaxSize()) {
        ArcCanvas(
            outer = outer,
            inner = inner,
            outerColor = outerColor,
            innerColor = inks.inner,
            segmentColors = inks.segments,
            glow = account.platformId == "cursor" && outerBreached,
            typeface = typeface,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = scale.dp(WatchBoardScale.SAFE_INSET_PX))
                .offset(y = -scale.dp(if (grokBot) 54 else 26)),
        ) {
            if (grokBot) {
                Image(
                    painter = painterResource(R.drawable.grok_bot_mark),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(scale.dp(84))
                        .clip(RoundedCornerShape(scale.dp(22))),
                )
                Text(
                    text = account.displayName,
                    style = WatchText.grokBotName,
                    modifier = Modifier.padding(top = scale.dp(10)),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scale.dp(5)),
                ) {
                    if (work) {
                        Text("W", style = WatchText.workTick)
                    }
                    Box(
                        Modifier
                            .size(scale.dp(11))
                            .background(nameSwatch),
                    )
                    Text(account.displayName, style = WatchText.platformName)
                }
            }
            Text(
                text = stringResource(
                    R.string.watch_percent,
                    WatchArcMath.centrePercent(account),
                ),
                style = (if (grokBot) WatchText.grokBotFigure else WatchText.figure).copy(
                    color = figureColor,
                    shadow = if (account.platformId == "cursor" && outerBreached) {
                        Shadow(
                            color = WatchInks.cursorBreach.copy(alpha = 0.5f),
                            blurRadius = 14f,
                        )
                    } else {
                        Shadow(color = Color.Transparent)
                    },
                ),
                modifier = Modifier.padding(
                    top = scale.dp(
                        when {
                            grokBot -> 3f
                            grokParts -> 4f
                            else -> 6f
                        },
                    ),
                ),
            )
            Box(
                Modifier
                    .padding(
                        vertical = scale.dp(
                            when {
                                grokBot -> 12f
                                grokParts -> 13f
                                else -> 14f
                            },
                        ),
                    )
                    .width(scale.dp(88))
                    .height(scale.dp(1))
                    .background(WatchInks.rule),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = resetTopPadding)
                .padding(horizontal = scale.dp(WatchBoardScale.SAFE_INSET_PX)),
        ) {
            if (grokParts) {
                Column(
                    modifier = Modifier.width(scale.dp(118)),
                    verticalArrangement = Arrangement.spacedBy(scale.dp(5)),
                ) {
                    outer?.segments.orEmpty().forEachIndexed { index, part ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(scale.dp(7)),
                        ) {
                            Box(
                                Modifier
                                    .size(scale.dp(7))
                                    .background(
                                        inks.segments.getOrElse(index) { outerColor },
                                    ),
                            )
                            Text(part.label, style = WatchText.grokPart)
                            Text(
                                text = stringResource(R.string.watch_percent, part.percent.toInt()),
                                style = WatchText.grokPartValue,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                }
            } else {
                val reset = centre?.let {
                    WearResetCopy.centreReset(
                        remainingMs = it.remainingMs,
                        resetLabel = it.resetLabel,
                    )
                }.orEmpty()
                if (reset.isNotBlank()) {
                    Text(
                        reset,
                        style = if (grokBot) WatchText.grokBotReset else WatchText.reset,
                        textAlign = TextAlign.Center,
                    )
                }
                if (account.viaLine.isNotBlank()) {
                    Text(
                        account.viaLine,
                        style = WatchText.via,
                        modifier = Modifier.padding(top = scale.dp(3)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcCanvas(
    outer: WearWindow?,
    inner: WearWindow?,
    outerColor: Color,
    innerColor: Color,
    segmentColors: List<Color>,
    glow: Boolean,
    typeface: Typeface,
) {
    Canvas(Modifier.fillMaxSize()) {
        val scale = size.minDimension / WatchArcMath.VIEWBOX
        val origin = Offset(
            (size.width - WatchArcMath.VIEWBOX * scale) / 2f,
            (size.height - WatchArcMath.VIEWBOX * scale) / 2f,
        )
        val cx = origin.x + WatchArcMath.CENTER * scale
        val cy = origin.y + WatchArcMath.CENTER * scale
        fun r(value: Float) = value * scale

        fun track(radius: Float, stroke: Float) {
            drawCircle(
                color = WatchInks.track,
                radius = r(radius),
                center = Offset(cx, cy),
                style = Stroke(width = r(stroke), cap = StrokeCap.Butt),
            )
        }

        fun tick(radius: Float, stroke: Float, elapsed: Float) {
            val angle = Math.toRadians(WatchArcMath.tickAngleDegrees(elapsed).toDouble())
            val innerR = r(radius) + r(stroke) / 2f
            val outerR = innerR + r(6f)
            val c = Offset(cx, cy)
            drawLine(
                color = WatchInks.tick,
                start = Offset(
                    c.x + (cos(angle) * innerR).toFloat(),
                    c.y + (sin(angle) * innerR).toFloat(),
                ),
                end = Offset(
                    c.x + (cos(angle) * outerR).toFloat(),
                    c.y + (sin(angle) * outerR).toFloat(),
                ),
                strokeWidth = r(2f),
                cap = StrokeCap.Round,
            )
        }

        fun fillArc(
            radius: Float,
            stroke: Float,
            color: Color,
            sweep: Float,
            start: Float,
            glowArc: Boolean,
        ) {
            if (sweep <= 0f) return
            val style = Stroke(width = r(stroke), cap = StrokeCap.Butt)
            if (glowArc) {
                drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = r(radius),
                    center = Offset(cx, cy),
                    style = Stroke(width = r(stroke) + r(8f)),
                )
            }
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r(radius), cy - r(radius)),
                size = androidx.compose.ui.geometry.Size(r(radius) * 2f, r(radius) * 2f),
                style = style,
            )
        }

        track(WatchArcMath.OUTER_RADIUS, WatchArcMath.OUTER_STROKE)
        if (inner != null) {
            track(WatchArcMath.INNER_RADIUS, WatchArcMath.INNER_STROKE)
        }

        val segments = outer?.segments.orEmpty()
        if (segments.isNotEmpty() && inner == null) {
            var start = WatchArcMath.START_ANGLE_DEGREES
            segments.forEachIndexed { index, segment ->
                val sweep = WatchArcMath.sweepDegrees(segment.percent)
                val color = segmentColors.getOrElse(index) { outerColor }
                fillArc(
                    WatchArcMath.OUTER_RADIUS,
                    WatchArcMath.OUTER_STROKE,
                    color,
                    sweep,
                    start,
                    glowArc = false,
                )
                start += sweep
            }
        } else if (outer != null) {
            fillArc(
                WatchArcMath.OUTER_RADIUS,
                WatchArcMath.OUTER_STROKE,
                outerColor,
                WatchArcMath.sweepDegrees(outer.usagePercent),
                WatchArcMath.START_ANGLE_DEGREES,
                glow,
            )
        }
        if (inner != null) {
            fillArc(
                WatchArcMath.INNER_RADIUS,
                WatchArcMath.INNER_STROKE,
                innerColor,
                WatchArcMath.sweepDegrees(inner.usagePercent),
                WatchArcMath.START_ANGLE_DEGREES,
                glowArc = false,
            )
        }
        outer?.let { tick(WatchArcMath.OUTER_RADIUS, WatchArcMath.OUTER_STROKE, it.elapsedPercent) }
        inner?.let { tick(WatchArcMath.INNER_RADIUS, WatchArcMath.INNER_STROKE, it.elapsedPercent) }

        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = 11f * scale
                letterSpacing = 0.04f
                textAlign = Paint.Align.LEFT
            }
            fun label(window: WearWindow?, radius: Float, color: Color) {
                if (window == null) return
                paint.color = color.toArgb()
                val path = android.graphics.Path()
                val rr = r(radius)
                path.addArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), 180f, -180f)
                val text = WatchArcMath.curveLabel(window)
                val measure = android.graphics.PathMeasure(path, false)
                val hOffset = ((measure.length - paint.measureText(text)) / 2f).coerceAtLeast(0f)
                canvas.nativeCanvas.drawTextOnPath(
                    text,
                    path,
                    hOffset,
                    0f,
                    paint,
                )
            }
            val outerLabel = if (inner == null) WatchInks.tick else outerColor
            label(outer, WatchArcMath.OUTER_LABEL_RADIUS, outerLabel)
            label(inner, WatchArcMath.INNER_LABEL_RADIUS, innerColor)
        }
    }
}
