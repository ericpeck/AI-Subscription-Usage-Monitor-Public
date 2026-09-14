package com.ericmbpeck.ai_sum.ui.info

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.ui.components.SectionRule
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.Accent
import com.ericmbpeck.ai_sum.ui.theme.Accent700
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral500
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.SourceSerif4
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold

@Composable
fun InfoScreen(
    onBack: () -> Unit,
    onDonate: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        InfoHeader(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.info_what_title),
                style = BroadsheetText.infoHeading,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.info_what_body),
                style = BroadsheetText.infoBody,
            )
            Text(
                text = stringResource(R.string.info_philosophy_title),
                style = BroadsheetText.infoHeading,
                modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.info_philosophy_before))
                    withStyle(
                        SpanStyle(
                            fontFamily = SourceSerif4,
                            fontStyle = FontStyle.Italic,
                            color = Ink,
                        ),
                    ) {
                        append(stringResource(R.string.info_philosophy_quote))
                    }
                    append(stringResource(R.string.info_philosophy_after))
                },
                style = BroadsheetText.infoBody,
            )
            Text(
                text = stringResource(R.string.info_faq_title),
                style = BroadsheetText.infoHeading,
                modifier = Modifier.padding(top = 26.dp, bottom = 10.dp),
            )
            FaqItem(
                question = stringResource(R.string.info_faq_signin_q),
                answer = stringResource(R.string.info_faq_signin_a),
            )
            FaqItem(
                question = stringResource(R.string.info_faq_data_q),
                answer = stringResource(R.string.info_faq_data_a),
            )
            FaqItem(
                question = stringResource(R.string.info_faq_twice_q),
                answer = buildAnnotatedString {
                    append(stringResource(R.string.info_faq_twice_before))
                    withStyle(
                        SpanStyle(
                            fontFamily = SourceSerif4,
                            fontWeight = FontWeight.SemiBold,
                            color = WorkMarkGold,
                        ),
                    ) {
                        append(stringResource(R.string.work_tick))
                    }
                    append(stringResource(R.string.info_faq_twice_after))
                },
            )
            FaqItem(
                question = stringResource(R.string.info_faq_lag_q),
                answer = stringResource(R.string.info_faq_lag_a),
            )
            FaqItem(
                question = stringResource(R.string.info_faq_missing_q),
                answer = stringResource(R.string.info_faq_missing_a),
                last = true,
            )
            Text(
                text = stringResource(R.string.info_donate_title),
                style = BroadsheetText.infoHeading,
                modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.info_donate_body),
                style = BroadsheetText.infoBody,
                modifier = Modifier.padding(bottom = 14.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .background(Accent)
                        .clickable(role = Role.Button, onClick = onDonate),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_heart),
                            contentDescription = null,
                            tint = Paper,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_donate),
                            style = BroadsheetText.infoAction.copy(color = Paper),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .widthIn(min = 96.dp)
                        .heightIn(min = 46.dp)
                        .border(1.dp, Neutral500)
                        .clickable(role = Role.Button, onClick = onShare)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.action_share),
                        style = BroadsheetText.infoAction,
                    )
                }
            }
            SectionRule(Modifier.padding(top = 24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.info_version).uppercase(),
                    style = BroadsheetText.infoFooterMeta,
                )
                Text(
                    text = stringResource(R.string.info_date).uppercase(),
                    style = BroadsheetText.infoFooterMeta,
                )
            }
            Text(
                text = stringResource(R.string.info_legal),
                style = BroadsheetText.infoLegal,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun InfoHeader(onBack: () -> Unit) {
    val backInteraction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    indication = null,
                    interactionSource = backInteraction,
                    onClick = onBack,
                )
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.cd_back),
                tint = Accent700,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.info_back).uppercase(),
                style = BroadsheetText.screenMeta.copy(
                    color = Accent700,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.07.em,
                ),
            )
        }
        Text(
            text = stringResource(R.string.info_title),
            style = BroadsheetText.screenTitle,
        )
        Text(
            text = stringResource(R.string.info_meta).uppercase(),
            style = BroadsheetText.screenMeta,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        SectionRule()
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    last: Boolean = false,
) {
    FaqItem(
        question = question,
        answer = buildAnnotatedString { append(answer) },
        last = last,
    )
}

@Composable
private fun FaqItem(
    question: String,
    answer: AnnotatedString,
    last: Boolean = false,
) {
    Column(Modifier.padding(bottom = if (last) 2.dp else 14.dp)) {
        Text(text = question, style = BroadsheetText.faqQuestion)
        Text(
            text = answer,
            style = BroadsheetText.faqAnswer,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun InfoScreenPreview() {
    AISUMTheme {
        InfoScreen(onBack = {}, onDonate = {}, onShare = {})
    }
}
