package com.ericmbpeck.ai_sum.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.data.fakeClaudeAccount
import com.ericmbpeck.ai_sum.data.withBoardIdentity
import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platform
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.ui.components.BroadsheetToggle
import com.ericmbpeck.ai_sum.ui.components.PlatformSwatch
import com.ericmbpeck.ai_sum.ui.components.SectionRule
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.SourceSerif4
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold

@Composable
fun SettingsPanel(
    account: Account,
    platform: Platform,
    onToggleNotify: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bar = Color(platform.barColor.toInt())
    val work = account.kind == AccountKind.WORK
    val emailLine = account.email.ifBlank {
        stringResource(R.string.settings_signed_in_on_phone)
    }
    val onOff = if (account.notifyOnReset) {
        stringResource(R.string.settings_state_on)
    } else {
        stringResource(R.string.settings_state_off)
    }
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_section_account).uppercase(),
            style = BroadsheetText.sectionHead,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlatformSwatch(color = bar)
            Column(Modifier.weight(1f)) {
                Text(
                    text = emailLine,
                    style = BroadsheetText.settingsEmail,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.accountDetailLine,
                    style = BroadsheetText.ledgerPlans,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (work) {
                Text(
                    text = stringResource(R.string.work_tick).uppercase(),
                    style = BroadsheetText.workTick.copy(color = WorkMarkGold),
                    modifier = Modifier.border(1.dp, WorkMarkGold).padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_section_notification).uppercase(),
            style = BroadsheetText.sectionHead,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_notify_title),
                    style = BroadsheetText.settingsNotifyTitle,
                )
                Text(
                    text = stringResource(
                        if (work) {
                            R.string.settings_notify_explainer_work
                        } else {
                            R.string.settings_notify_explainer_personal
                        },
                    ),
                    style = BroadsheetText.settingsExplainer,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            BroadsheetToggle(
                on = account.notifyOnReset,
                onColor = bar,
                onToggle = onToggleNotify,
            )
        }
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            WindowStateLabel(
                name = stringResource(R.string.settings_window_session),
                state = onOff,
            )
            WindowStateLabel(
                name = stringResource(R.string.settings_window_weekly),
                state = onOff,
            )
        }
        SectionRule(Modifier.padding(top = 22.dp))
        Text(
            text = stringResource(R.string.settings_footnote_tabs),
            style = BroadsheetText.footnote,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun WindowStateLabel(name: String, state: String) {
    Text(
        text = buildAnnotatedString {
            append(name)
            append(" ")
            withStyle(
                SpanStyle(
                    fontFamily = SourceSerif4,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                ),
            ) {
                append(state)
            }
        },
        style = BroadsheetText.settingsSubState,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun SettingsPanelPersonalPreview() {
    AISUMTheme {
        SettingsPanel(
            account = fakeClaudeAccount(1, AccountKind.PERSONAL).withBoardIdentity(),
            platform = Platforms.byId(Platforms.CLAUDE),
            onToggleNotify = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 372)
@Composable
private fun SettingsPanelWorkPreview() {
    AISUMTheme {
        SettingsPanel(
            account = fakeClaudeAccount(2, AccountKind.WORK).withBoardIdentity(),
            platform = Platforms.byId(Platforms.CLAUDE),
            onToggleNotify = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
