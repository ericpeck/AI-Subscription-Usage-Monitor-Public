package com.ericmbpeck.ai_sum.ui.platform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.data.fakeClaudeAccount
import com.ericmbpeck.ai_sum.data.fakeCodexAccount
import com.ericmbpeck.ai_sum.data.fakeCopilotAccount
import com.ericmbpeck.ai_sum.data.fakeCursorAccount
import com.ericmbpeck.ai_sum.data.fakeGeminiAccount
import com.ericmbpeck.ai_sum.data.fakeGrokAccount
import com.ericmbpeck.ai_sum.data.fakeGrokBotAccount
import com.ericmbpeck.ai_sum.data.fakeM365Account
import com.ericmbpeck.ai_sum.data.fakePerplexityAccount
import com.ericmbpeck.ai_sum.data.withBoardIdentity
import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platform
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.model.UsageWindow
import com.ericmbpeck.ai_sum.ui.components.PlatformTabStrip
import com.ericmbpeck.ai_sum.ui.components.ScreenHeader
import com.ericmbpeck.ai_sum.ui.components.SectionRule
import com.ericmbpeck.ai_sum.ui.components.TabItem
import com.ericmbpeck.ai_sum.ui.components.WindowRow
import com.ericmbpeck.ai_sum.ui.settings.RemoveConfirmOverlay
import com.ericmbpeck.ai_sum.ui.settings.SettingsPanel
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Destructive
import com.ericmbpeck.ai_sum.ui.theme.Divider
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral600
import com.ericmbpeck.ai_sum.ui.theme.Neutral700
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.SourceSerif4
import com.ericmbpeck.ai_sum.usage.LiveSignIn
import com.ericmbpeck.ai_sum.usage.ResetFormatter

data class PlatformUiState(
    val account: Account,
    val platform: Platform,
    val tabs: List<Account>,
)

@Composable
fun PlatformScreen(
    uiState: PlatformUiState,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    onTab: (Long) -> Unit,
    modifier: Modifier = Modifier,
    animationKey: Int = 0,
    onReauth: (() -> Unit)? = null,
    settingsMode: Boolean = false,
    onToggleSettings: () -> Unit = {},
    onToggleNotify: () -> Unit = {},
    onRemoveRequest: () -> Unit = {},
    showRemoveConfirm: Boolean = false,
    onDismissRemove: () -> Unit = {},
    onConfirmRemove: () -> Unit = {},
    onInfo: () -> Unit = {},
) {
    val platform = uiState.platform
    val account = uiState.account
    val bar = Color(platform.barColor.toInt())
    val figure = Color(platform.figureColor.toInt())
    val work = account.kind == AccountKind.WORK
    val signedOut = account.needsReauth && !settingsMode
    val lastRead = ResetFormatter.lastReadPhrase(account.updatedLabel)
    val hasSibling = uiState.tabs.any {
        it.platformId == account.platformId && it.id != account.id
    }
    val emailLine = account.email.ifBlank {
        stringResource(R.string.settings_signed_in_on_phone)
    }
    val meta = when {
        settingsMode && work -> stringResource(R.string.settings_meta_work)
        settingsMode -> stringResource(R.string.settings_meta_personal)
        signedOut -> stringResource(R.string.account_meta_reauth, lastRead)
        else -> stringResource(
            if (work) R.string.account_meta_work else R.string.account_meta_personal,
            account.updatedLabel,
        )
    }
    var expandedIndex by remember(account.id) { mutableStateOf<Int?>(null) }
    val tabs = uiState.tabs.map { tabAccount ->
        val tabPlatform = Platforms.byId(tabAccount.platformId)
        TabItem(
            accountId = tabAccount.id,
            label = tabPlatform.tabLabel,
            barColor = Color(tabPlatform.barColor.toInt()),
            figureColor = Color(tabPlatform.figureColor.toInt()),
            kind = tabAccount.kind,
            selected = tabAccount.id == account.id,
            expired = tabAccount.needsReauth,
        )
    }
    BackHandler(enabled = showRemoveConfirm || settingsMode) {
        if (showRemoveConfirm) {
            onDismissRemove()
        } else {
            onToggleSettings()
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(
                title = platform.displayName,
                meta = meta,
                work = work,
                titleSuffix = if (settingsMode) {
                    stringResource(R.string.settings_suffix)
                } else {
                    null
                },
                refreshTint = if (settingsMode || signedOut) null else figure,
                onRefresh = if (settingsMode || signedOut) null else onRefresh,
                showInfo = settingsMode,
                onInfo = if (settingsMode) onInfo else null,
                statusIcon = if (signedOut) R.drawable.ic_warning_circle else null,
                statusTint = Destructive,
                metaColor = if (signedOut) Destructive else Neutral600,
                ruleColor = if (signedOut) Destructive else Divider,
                ruleHeight = if (signedOut) 2.dp else 1.dp,
                gearTint = if (settingsMode) figure else Neutral600,
                onGear = onToggleSettings,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                if (settingsMode) {
                    SettingsPanel(
                        account = account,
                        platform = platform,
                        onToggleNotify = onToggleNotify,
                    )
                } else if (signedOut) {
                    SignedOutBody(
                        platformName = platform.displayName,
                        hostLabel = LiveSignIn.hostLabel(platform.id),
                        lastRead = lastRead,
                        windows = account.windows,
                        bar = bar,
                        figure = figure,
                        onSignIn = { onReauth?.invoke() },
                    )
                } else {
                    PlanLine(
                        platformId = platform.id,
                        planLine = account.planLine,
                        modifier = Modifier.padding(bottom = 18.dp),
                    )
                    account.windows.forEachIndexed { index, window ->
                        WindowRow(
                            window = window,
                            barColor = bar,
                            figureColor = figure,
                            expanded = expandedIndex == index,
                            onToggle = {
                                expandedIndex = if (expandedIndex == index) null else index
                            },
                            animationKey = animationKey,
                        )
                    }
                    SectionRule()
                    Text(
                        text = account.footNote,
                        style = BroadsheetText.footnote,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
            }
            PlatformTabStrip(
                tabs = tabs,
                onAdd = onAdd,
                onTab = onTab,
                settingsMode = settingsMode,
                onRemove = onRemoveRequest,
            )
        }
        if (showRemoveConfirm) {
            RemoveConfirmOverlay(
                platformName = platform.displayName,
                emailLine = emailLine,
                hasSiblingSamePlatform = hasSibling,
                onCancel = onDismissRemove,
                onConfirm = onConfirmRemove,
            )
        }
    }
}

@Composable
private fun SignedOutBody(
    platformName: String,
    hostLabel: String,
    lastRead: String,
    windows: List<UsageWindow>,
    bar: Color,
    figure: Color,
    onSignIn: () -> Unit,
) {
    Text(
        text = stringResource(R.string.signed_out_headline, platformName),
        style = BroadsheetText.signedOutHeadline,
        modifier = Modifier.padding(top = 6.dp),
    )
    Text(
        text = stringResource(R.string.signed_out_body),
        style = BroadsheetText.emptyBody,
        modifier = Modifier.padding(top = 12.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .heightIn(min = 50.dp)
            .background(Destructive)
            .clickable(role = Role.Button, onClick = onSignIn),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sign_in),
            contentDescription = null,
            tint = Paper,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(19.dp),
        )
        Text(
            text = stringResource(R.string.action_sign_in_to, platformName),
            style = BroadsheetText.getStarted,
        )
    }
    Text(
        text = stringResource(R.string.signed_out_caption, hostLabel),
        style = BroadsheetText.footnote.copy(lineHeight = (12 * 1.55).sp),
        modifier = Modifier.padding(top = 10.dp),
    )
    SectionRule(Modifier.padding(top = 24.dp))
    Text(
        text = stringResource(R.string.signed_out_last_read, lastRead).uppercase(),
        style = BroadsheetText.screenMeta.copy(color = Neutral700),
        modifier = Modifier.padding(top = 18.dp, bottom = 14.dp),
    )
    windows.forEach { window ->
        WindowRow(
            window = window,
            barColor = bar,
            figureColor = figure,
            expanded = false,
            onToggle = {},
            dimmed = true,
        )
    }
}

@Composable
private fun PlanLine(
    platformId: String,
    planLine: String,
    modifier: Modifier = Modifier,
) {
    if (platformId == Platforms.GROK_BOT) {
        val headline = planLine.substringBefore('\n')
        val billedFromPlan = planLine.substringAfter('\n', missingDelimiterValue = "").trim()
        val parent = headline.substringAfter("Included with ", missingDelimiterValue = "")
            .ifBlank { stringResource(R.string.grok_bot_plan_parent) }
        val billed = billedFromPlan.ifBlank {
            if (parent.contains("Cursor", ignoreCase = true)) {
                stringResource(R.string.grok_bot_plan_billed_cursor)
            } else {
                stringResource(R.string.grok_bot_plan_billed)
            }
        }
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.grok_bot_plan_included_prefix))
                append(" ")
                withStyle(
                    SpanStyle(
                        fontFamily = SourceSerif4,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                    ),
                ) {
                    append(parent)
                }
                append("\n")
                append(billed)
            },
            style = BroadsheetText.planLine.copy(lineHeight = (13 * 1.45).sp),
            modifier = modifier,
        )
    } else {
        Text(
            text = planLine,
            style = BroadsheetText.planLine,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlatformPreview(account: Account, platformId: String) {
    AISUMTheme {
        PlatformScreen(
            uiState = PlatformUiState(
                account = account,
                platform = Platforms.byId(platformId),
                tabs = listOf(account),
            ),
            onRefresh = {},
            onAdd = {},
            onTab = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun CursorPlatformPreview() {
    PlatformPreview(fakeCursorAccount(1, AccountKind.PERSONAL), Platforms.CURSOR)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun ClaudePlatformPreview() {
    PlatformPreview(fakeClaudeAccount(1, AccountKind.PERSONAL), Platforms.CLAUDE)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun GeminiPlatformPreview() {
    PlatformPreview(fakeGeminiAccount(1, AccountKind.PERSONAL), Platforms.GEMINI)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun GrokPlatformPreview() {
    PlatformPreview(fakeGrokAccount(1, AccountKind.PERSONAL), Platforms.GROK)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun GrokBotPlatformPreview() {
    PlatformPreview(fakeGrokBotAccount(1, AccountKind.PERSONAL), Platforms.GROK_BOT)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun CodexPlatformPreview() {
    PlatformPreview(fakeCodexAccount(1, AccountKind.PERSONAL), Platforms.CODEX)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun PerplexityPlatformPreview() {
    PlatformPreview(fakePerplexityAccount(1, AccountKind.PERSONAL), Platforms.PERPLEXITY)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun CopilotPlatformPreview() {
    PlatformPreview(fakeCopilotAccount(1, AccountKind.PERSONAL), Platforms.COPILOT)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun M365PlatformPreview() {
    PlatformPreview(fakeM365Account(1, AccountKind.WORK), Platforms.M365)
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun ClaudeSignedOutPreview() {
    val claude = fakeClaudeAccount(1, AccountKind.PERSONAL).copy(needsReauth = true)
    val cursor = fakeCursorAccount(2, AccountKind.PERSONAL)
    val m365 = fakeM365Account(3, AccountKind.WORK)
    AISUMTheme {
        PlatformScreen(
            uiState = PlatformUiState(
                account = claude,
                platform = Platforms.byId(Platforms.CLAUDE),
                tabs = listOf(claude, cursor, m365),
            ),
            onRefresh = {},
            onAdd = {},
            onTab = {},
            onReauth = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun ClaudeSettingsPersonalPreview() {
    AISUMTheme {
        PlatformScreen(
            uiState = PlatformUiState(
                account = fakeClaudeAccount(1, AccountKind.PERSONAL).withBoardIdentity(),
                platform = Platforms.byId(Platforms.CLAUDE),
                tabs = listOf(
                    fakeClaudeAccount(1, AccountKind.PERSONAL).withBoardIdentity(),
                    fakeM365Account(2, AccountKind.WORK).withBoardIdentity(),
                ),
            ),
            onRefresh = {},
            onAdd = {},
            onTab = {},
            settingsMode = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun ClaudeSettingsWorkPreview() {
    AISUMTheme {
        PlatformScreen(
            uiState = PlatformUiState(
                account = fakeM365Account(2, AccountKind.WORK).withBoardIdentity(),
                platform = Platforms.byId(Platforms.M365),
                tabs = listOf(
                    fakeClaudeAccount(1, AccountKind.PERSONAL).withBoardIdentity(),
                    fakeM365Account(2, AccountKind.WORK).withBoardIdentity(),
                ),
            ),
            onRefresh = {},
            onAdd = {},
            onTab = {},
            settingsMode = true,
            showRemoveConfirm = true,
        )
    }
}
