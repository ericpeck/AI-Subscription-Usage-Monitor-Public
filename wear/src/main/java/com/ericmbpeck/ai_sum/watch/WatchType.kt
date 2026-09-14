package com.ericmbpeck.ai_sum.watch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import com.ericmbpeck.ai_sum.R

val SourceSerif4 = FontFamily(
    Font(R.font.source_serif_4_regular, FontWeight.Normal),
    Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
)

private val tightLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private val noFontPad = PlatformTextStyle(includeFontPadding = false)

object WatchText {
    val emptyHeadline: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            val size = scale.sp(24)
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = size,
                lineHeight = scale.sp(24 * 1.2f),
                letterSpacing = (-0.02).em,
                color = Color(0xFFE8E5E1),
                platformStyle = noFontPad,
            )
        }

    val emptyBody: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(14),
                lineHeight = scale.sp(14 * 1.45f),
                color = WatchInks.secondary,
                platformStyle = noFontPad,
            )
        }

    val emptyButton: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = scale.sp(15),
                color = WatchInks.emptyButtonInk,
                platformStyle = noFontPad,
            )
        }

    val emptyCaption: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(11),
                color = WatchInks.muted,
                platformStyle = noFontPad,
            )
        }

    val platformName: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            val size = scale.sp(23)
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = size,
                lineHeight = size,
                letterSpacing = (-0.015).em,
                color = WatchInks.name,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val grokBotName: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            val size = scale.sp(20)
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = size,
                lineHeight = size,
                letterSpacing = (-0.015).em,
                color = WatchInks.name,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val figure: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            val size = scale.sp(76)
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = size,
                lineHeight = size,
                letterSpacing = (-0.045).em,
                fontFeatureSettings = "tnum",
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val grokBotFigure: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            val size = scale.sp(64)
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = size,
                lineHeight = size,
                letterSpacing = (-0.045).em,
                fontFeatureSettings = "tnum",
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val reset: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(14),
                lineHeight = scale.sp(14),
                color = WatchInks.reset,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val grokBotReset: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(13),
                lineHeight = scale.sp(13),
                color = WatchInks.reset,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val via: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(11),
                lineHeight = scale.sp(11),
                color = WatchInks.muted,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val grokPart: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Normal,
                fontSize = scale.sp(14),
                lineHeight = scale.sp(14),
                color = WatchInks.reset,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val grokPartValue: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.SemiBold,
                fontSize = scale.sp(14),
                lineHeight = scale.sp(14),
                fontFeatureSettings = "tnum",
                color = Color(0xFFF0ECE7),
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }

    val workTick: TextStyle
        @Composable
        @ReadOnlyComposable
        get() {
            val scale = LocalWatchScale.current
            return TextStyle(
                fontFamily = SourceSerif4,
                fontWeight = FontWeight.Bold,
                fontSize = scale.sp(11),
                lineHeight = scale.sp(11),
                color = WatchInks.workGold,
                platformStyle = noFontPad,
                lineHeightStyle = tightLines,
            )
        }
}
