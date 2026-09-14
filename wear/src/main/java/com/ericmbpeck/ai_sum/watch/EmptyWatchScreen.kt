package com.ericmbpeck.ai_sum.watch

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.Text
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.wearbridge.WatchBoardScale
import kotlin.math.cos
import kotlin.math.sin

private data class Star(val x: Float, val y: Float, val r: Float, val opacity: Float)
private data class Shoot(
    val x: Float,
    val y: Float,
    val dx: Float,
    val dy: Float,
    val durationMs: Int,
    val delayMs: Int,
)

@Composable
fun EmptyWatchScreen(
    onOpenPhone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    val scale = LocalWatchScale.current
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        EmptySky(reduceMotion = reduceMotion)
        Column(
            modifier = Modifier.padding(horizontal = scale.dp(WatchBoardScale.SAFE_INSET_PX)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhoneGlyph(size = scale.dp(40))
            Text(
                text = stringResource(R.string.watch_empty_headline),
                style = WatchText.emptyHeadline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = scale.dp(14)),
            )
            Text(
                text = stringResource(R.string.watch_empty_body),
                style = WatchText.emptyBody,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = scale.dp(10)),
            )
            Row(
                modifier = Modifier
                    .padding(top = scale.dp(18))
                    .heightIn(min = scale.dp(46))
                    .clip(RoundedCornerShape(scale.dp(23)))
                    .background(WatchInks.emptyButton)
                    .clickable(role = Role.Button, onClick = onOpenPhone)
                    .padding(horizontal = scale.dp(22)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(scale.dp(8)),
            ) {
                PhoneGlyph(color = WatchInks.emptyButtonInk, size = scale.dp(18))
                Text(
                    text = stringResource(R.string.watch_open_on_phone),
                    style = WatchText.emptyButton,
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(R.string.watch_open_caption),
                style = WatchText.emptyCaption,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = scale.dp(9)),
            )
        }
    }
}

@Composable
private fun EmptySky(reduceMotion: Boolean) {
    val stars = remember { starField }
    val shoots = remember { shootingStars }
    val twinkle = if (reduceMotion) {
        0.4f
    } else {
        val t = rememberInfiniteTransition(label = "twinkle")
        t.animateFloat(
            initialValue = 0.26f,
            targetValue = 0.72f,
            animationSpec = infiniteRepeatable(
                animation = tween(3800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "twinkle-alpha",
        ).value
    }
    val saucerT = if (reduceMotion) {
        -1f
    } else {
        val t = rememberInfiniteTransition(label = "saucer")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(60_000, delayMillis = 6_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "saucer-t",
        ).value
    }
    val shootPhase = if (reduceMotion) {
        0f
    } else {
        val t = rememberInfiniteTransition(label = "shoot")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shoot-t",
        ).value
    }
    Canvas(Modifier.fillMaxSize()) {
        val scale = size.minDimension / 380f
        val origin = Offset((size.width - 380f * scale) / 2f, (size.height - 380f * scale) / 2f)
        fun map(x: Float, y: Float): Offset = Offset(origin.x + x * scale, origin.y + y * scale)
        val clipR = 158f * scale
        val center = map(190f, 190f)
        stars.forEach { star ->
            val at = map(star.x, star.y)
            if ((at - center).getDistance() <= clipR) {
                drawCircle(
                    color = Color(0xFFDEDAD5).copy(alpha = star.opacity * twinkle / 0.5f),
                    radius = star.r * scale,
                    center = at,
                )
            }
        }
        if (!reduceMotion) {
            shoots.forEachIndexed { index, shoot ->
                val local = ((shootPhase + index * 0.12f) % 1f)
                val visible = local in 0.05f..0.35f
                if (visible) {
                    val p = (local - 0.05f) / 0.30f
                    val start = map(shoot.x, shoot.y)
                    val end = Offset(start.x + shoot.dx * scale, start.y + shoot.dy * scale)
                    val head = Offset(
                        start.x + (end.x - start.x) * p,
                        start.y + (end.y - start.y) * p,
                    )
                    val tail = Offset(
                        start.x + (end.x - start.x) * (p - 0.25f).coerceAtLeast(0f),
                        start.y + (end.y - start.y) * (p - 0.25f).coerceAtLeast(0f),
                    )
                    if ((head - center).getDistance() <= clipR) {
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x007CC6DD), Color(0xFFB8E4F2)),
                                start = tail,
                                end = head,
                            ),
                            start = tail,
                            end = head,
                            strokeWidth = 2.6f * scale,
                            cap = StrokeCap.Round,
                        )
                        drawCircle(Color(0xFFEAF7FC), 2.6f * scale, head)
                    }
                }
            }
            if (saucerT in 0.014f..0.066f) {
                val u = (saucerT - 0.014f) / (0.066f - 0.014f)
                val x = 326f - 278f * u
                val y = 190f - 5f * sin(u * Math.PI).toFloat()
                val at = map(x, y)
                if ((at - center).getDistance() <= clipR) {
                    drawOval(
                        color = Color(0xFF8F9AA0),
                        topLeft = Offset(at.x - 15f * scale, at.y - 4.2f * scale),
                        size = androidx.compose.ui.geometry.Size(30f * scale, 8.4f * scale),
                    )
                    drawOval(
                        color = Color(0xFFB9C4CA),
                        topLeft = Offset(at.x - 15f * scale, at.y - 5.6f * scale),
                        size = androidx.compose.ui.geometry.Size(30f * scale, 6.4f * scale),
                    )
                    val light = Color(0xFFEAF7FC)
                    drawCircle(light, 1.5f * scale, Offset(at.x - 9.5f * scale, at.y + 1.3f * scale))
                    drawCircle(light, 1.5f * scale, Offset(at.x, at.y + 2f * scale))
                    drawCircle(light, 1.5f * scale, Offset(at.x + 9.5f * scale, at.y + 1.3f * scale))
                }
            }
        }
        drawCircle(
            color = WatchInks.track,
            radius = 168f * scale,
            center = center,
            style = Stroke(
                width = 12f * scale,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(3f * scale, 13f * scale),
                ),
            ),
        )
    }
}

@Composable
private fun PhoneGlyph(
    color: Color = WatchInks.muted,
    size: Dp,
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width * 0.55f
        val h = this.size.height * 0.80f
        val stroke = this.size.minDimension * 0.08f
        drawRoundRect(
            color = color,
            topLeft = Offset((this.size.width - w) / 2f, (this.size.height - h) / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = Stroke(width = stroke),
        )
    }
}

private val starField = listOf(
    Star(105.5f, 296.6f, 0.87f, 0.60f),
    Star(256.9f, 222.1f, 1.14f, 0.35f),
    Star(239f, 313.5f, 1.27f, 0.43f),
    Star(186.5f, 226.6f, 1.29f, 0.25f),
    Star(125.5f, 149.1f, 1.39f, 0.29f),
    Star(66.6f, 256.9f, 1.54f, 0.61f),
    Star(279.9f, 88.9f, 1.68f, 0.43f),
    Star(50.6f, 245.4f, 1.59f, 0.52f),
    Star(158.5f, 318f, 1.44f, 0.55f),
    Star(320.1f, 256.3f, 1.06f, 0.49f),
    Star(77f, 163f, 1.66f, 0.51f),
    Star(166.4f, 100.1f, 0.88f, 0.31f),
    Star(228.9f, 91.5f, 1.17f, 0.54f),
    Star(99.6f, 124.4f, 1.6f, 0.39f),
    Star(262.6f, 183.9f, 1.03f, 0.34f),
    Star(140.4f, 182.9f, 1.46f, 0.32f),
    Star(66.6f, 203.6f, 1.25f, 0.50f),
    Star(183f, 330.1f, 1.64f, 0.48f),
    Star(269.2f, 309.8f, 1.03f, 0.56f),
    Star(120f, 99.5f, 1.69f, 0.56f),
    Star(103f, 70.1f, 1.64f, 0.54f),
    Star(158f, 80f, 1.1f, 0.40f),
    Star(296f, 178f, 1.1f, 0.40f),
    Star(88f, 166f, 1.0f, 0.40f),
    Star(196f, 292f, 1.1f, 0.40f),
)

private val shootingStars = listOf(
    Shoot(62f, 96f, 50f, 32f, 2600, 0),
    Shoot(90f, 70f, 59f, 62f, 5800, 900),
    Shoot(40f, 180f, 48f, 63f, 2400, 340),
    Shoot(200f, 50f, 55f, 43f, 7000, 980),
    Shoot(300f, 90f, 40f, 70f, 3400, 480),
    Shoot(70f, 250f, 57f, 47f, 5500, 770),
    Shoot(250f, 60f, 57f, 63f, 2800, 390),
    Shoot(110f, 40f, 60f, 47f, 4400, 620),
)
