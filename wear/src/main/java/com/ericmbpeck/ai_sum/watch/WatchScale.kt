package com.ericmbpeck.ai_sum.watch

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericmbpeck.ai_sum.wearbridge.WatchBoardScale
import kotlin.math.min

data class WatchScale(
    val screenMinPx: Float,
    val density: Float,
) {
    fun dp(boardPx: Float): Dp =
        WatchBoardScale.dpValue(boardPx, screenMinPx, density).dp

    fun dp(boardPx: Int): Dp = dp(boardPx.toFloat())

    fun sp(boardPx: Float): TextUnit =
        WatchBoardScale.spValue(boardPx, screenMinPx, density).sp

    fun sp(boardPx: Int): TextUnit = sp(boardPx.toFloat())
}

val LocalWatchScale = staticCompositionLocalOf {
    WatchScale(screenMinPx = WatchBoardScale.FACE_PX, density = 2f)
}

@Composable
fun ProvideWatchScale(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val minPx = with(density) { min(maxWidth.toPx(), maxHeight.toPx()) }
        val scale = remember(minPx, density.density) {
            WatchScale(minPx, density.density)
        }
        CompositionLocalProvider(LocalWatchScale provides scale) {
            content()
        }
    }
}
