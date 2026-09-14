package com.ericmbpeck.ai_sum.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ericmbpeck.ai_sum.R

val SourceSerif4 = FontFamily(
    Font(R.font.source_serif_4_regular, FontWeight.Normal),
    Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
)

object BroadsheetText {
    val screenTitle = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.025).em,
        color = Ink,
    )
    val emptyHeadline = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 27.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.02).em,
        color = Ink,
    )
    val screenMeta = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.1.em,
        color = Neutral600,
    )
    val planLine = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = Neutral700,
    )
    val windowName = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        color = Ink,
    )
    val usageFigure = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        letterSpacing = (-0.02).em,
        fontFeatureSettings = "tnum",
    )
    val resetLabel = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Neutral700,
    )
    val footnote = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = (12 * 1.6).sp,
        color = Neutral600,
    )
    val tabLabel = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = (-0.01).em,
    )
    val addLabel = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.04.em,
        color = Accent700,
    )
    val ledgerName = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = Ink,
    )
    val ledgerPlans = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = Neutral600,
    )
    val ledgerStatus = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.09.em,
    )
    val signedOutHeadline = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = (25 * 1.15).sp,
        letterSpacing = (-0.02).em,
        color = Ink,
    )
    val getStarted = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Paper,
    )
    val emptyBody = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.6).sp,
        color = Neutral700,
    )
    val workTick = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.07.em,
        color = Neutral600,
    )
    val pace = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        fontStyle = FontStyle.Italic,
        color = Neutral800,
    )
    val tabEmptyHint = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = Neutral700,
    )
    val signInTitle = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 31.sp,
        letterSpacing = (-0.025).em,
        color = Ink,
    )
    val signInLead = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = (15 * 1.6).sp,
        color = Ink,
    )
    val signInChrome = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = Neutral700,
    )
    val signInContinue = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = Color(0xFF1A1817),
    )
    val assurance = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.5).sp,
        color = Neutral800,
    )
    val legendLabel = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = Neutral800,
    )
    val legendValue = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
        color = Neutral800,
    )
    val titleSuffix = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = Neutral600,
    )
    val sectionHead = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.08.em,
        color = Neutral700,
    )
    val settingsEmail = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        color = Ink,
    )
    val settingsNotifyTitle = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Ink,
    )
    val settingsExplainer = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.55).sp,
        color = Neutral700,
    )
    val settingsSubState = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = Neutral700,
    )
    val removeTitle = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = Ink,
    )
    val removeBody = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.55).sp,
        color = Neutral800,
    )
    val removeAction = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = Ink,
    )
    val removeCaption = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = (11 * 1.5).sp,
        color = Neutral600,
    )
    val infoHeading = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        letterSpacing = (-0.015).em,
        color = Ink,
    )
    val infoBody = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.6).sp,
        color = Neutral800,
    )
    val faqQuestion = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = Ink,
    )
    val faqAnswer = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.55).sp,
        color = Neutral700,
    )
    val infoAction = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Ink,
    )
    val infoFooterMeta = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.09.em,
        color = Neutral600,
    )
    val infoLegal = TextStyle(
        fontFamily = SourceSerif4,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = (11 * 1.6).sp,
        color = Neutral600,
    )
}
