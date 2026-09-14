package com.ericmbpeck.ai_sum.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.data.fakeClaudeAccount
import com.ericmbpeck.ai_sum.data.fakeCursorAccount
import com.ericmbpeck.ai_sum.data.fakeM365Account
import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platform
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.ui.components.PlatformSwatch
import com.ericmbpeck.ai_sum.ui.components.PlatformTabStrip
import com.ericmbpeck.ai_sum.ui.components.ScreenHeader
import com.ericmbpeck.ai_sum.ui.components.TabItem
import com.ericmbpeck.ai_sum.ui.components.WorkTick
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.Accent700
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Destructive
import com.ericmbpeck.ai_sum.ui.theme.Divider
import com.ericmbpeck.ai_sum.ui.theme.Neutral600
import com.ericmbpeck.ai_sum.ui.theme.Neutral800
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.SourceSerif4
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold

private enum class LedgerRowState {
    Available,
    Active,
    Expired,
}

@Composable
fun LedgerScreen(
    accounts: List<Account>,
    onAddPlatform: (platformId: String, work: Boolean) -> Unit,
    onAdd: () -> Unit,
    onTab: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workTicks = remember { mutableStateMapOf<String, Boolean>() }
    val byPlatform = accounts.associateBy { it.platformId }
    val expiredCount = accounts.count { it.needsReauth }
    val connected = accounts.isNotEmpty()
    val tabs = accounts.map { account ->
        val platform = Platforms.byId(account.platformId)
        TabItem(
            accountId = account.id,
            label = platform.tabLabel,
            barColor = Color(platform.barColor.toInt()),
            figureColor = Color(platform.figureColor.toInt()),
            kind = account.kind,
            selected = false,
            expired = account.needsReauth,
        )
    }
    val title = if (connected) {
        stringResource(R.string.ledger_title_add)
    } else {
        stringResource(R.string.screen_title_usage)
    }
    val meta = when {
        expiredCount == 1 -> stringResource(R.string.ledger_meta_expired_one)
        expiredCount > 1 -> stringResource(R.string.ledger_meta_expired_many, expiredCount)
        connected -> stringResource(R.string.ledger_meta_connected, accounts.size)
        else -> stringResource(R.string.ledger_meta)
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = title,
            meta = meta,
            metaColor = if (expiredCount > 0) Destructive else Neutral600,
            onGear = {},
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Platforms.catalog.forEach { platform ->
                val existing = byPlatform[platform.id]
                if (!platform.addableThisSlice && existing == null) {
                    return@forEach
                }
                val state = when {
                    existing == null -> LedgerRowState.Available
                    existing.needsReauth -> LedgerRowState.Expired
                    else -> LedgerRowState.Active
                }
                val work = workTicks[platform.id] == true
                LedgerRow(
                    platform = platform,
                    state = state,
                    account = existing,
                    workSelected = work,
                    onWorkToggle = { workTicks[platform.id] = !work },
                    onAdd = {
                        if (platform.addableThisSlice) {
                            onAddPlatform(platform.id, work)
                        }
                    },
                    onLogInAgain = { existing?.let { onTab(it.id) } },
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Divider),
            )
            if (expiredCount == 0) {
                LedgerFootnote(connected = connected)
            }
        }
        PlatformTabStrip(
            tabs = tabs,
            onAdd = onAdd,
            onTab = onTab,
            addSelected = connected,
            emptyMessage = if (accounts.isEmpty()) {
                stringResource(R.string.ledger_nothing_added)
            } else {
                null
            },
        )
    }
}

@Composable
private fun LedgerFootnote(connected: Boolean) {
    val raw = stringResource(
        if (connected) R.string.ledger_footnote_connected else R.string.ledger_footnote,
    )
    Text(
        text = goldWork(raw),
        style = BroadsheetText.footnote,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun goldWork(text: String): AnnotatedString {
    val work = stringResource(R.string.work_tick)
    val workStyle = SpanStyle(
        color = WorkMarkGold,
        fontWeight = FontWeight.SemiBold,
        fontFamily = SourceSerif4,
    )
    val index = text.indexOf(work)
    return buildAnnotatedString {
        if (index < 0) {
            append(text)
            return@buildAnnotatedString
        }
        append(text.substring(0, index))
        withStyle(workStyle) { append(work) }
        append(text.substring(index + work.length))
    }
}

@Composable
private fun LedgerRow(
    platform: Platform,
    state: LedgerRowState,
    account: Account?,
    workSelected: Boolean,
    onWorkToggle: () -> Unit,
    onAdd: () -> Unit,
    onLogInAgain: () -> Unit,
) {
    val swatch = Color(platform.barColor.toInt())
    val plans = when (state) {
        LedgerRowState.Available -> AnnotatedString(platform.seedPlans)
        LedgerRowState.Active,
        LedgerRowState.Expired,
        -> connectedPlans(account, platform)
    }
    val topRule = if (state == LedgerRowState.Expired) 2.dp else 1.dp
    val topColor = if (state == LedgerRowState.Expired) Destructive else Divider
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(topRule)
                .background(topColor),
        )
        if (state == LedgerRowState.Expired) {
            ExpiredLedgerRow(
                name = platform.displayName,
                plans = plans,
                swatch = swatch,
                tabLabel = platform.tabLabel,
                onLogInAgain = onLogInAgain,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp)
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformSwatch(color = swatch)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(text = platform.displayName, style = BroadsheetText.ledgerName)
                    Text(text = plans, style = BroadsheetText.ledgerPlans)
                    if (platform.id == Platforms.CLAUDE && state == LedgerRowState.Available) {
                        Text(
                            text = stringResource(R.string.claude_free_disclaimer_short),
                            style = BroadsheetText.ledgerPlans,
                        )
                    }
                }
                if (state == LedgerRowState.Active) {
                    ActiveMark(color = Color(platform.figureColor.toInt()))
                } else {
                    WorkTick(selected = workSelected, onToggle = onWorkToggle)
                    Icon(
                        painter = painterResource(R.drawable.ic_plus_circle),
                        contentDescription = stringResource(R.string.cd_add),
                        tint = Accent700,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(role = Role.Button, onClick = onAdd)
                            .padding(11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveMark(color: Color) {
    val tint = if (color == Color(0xFF4F4C4C)) Neutral800 else color
    Row(
        modifier = Modifier.heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = stringResource(R.string.ledger_status_active).uppercase(),
            style = BroadsheetText.ledgerStatus.copy(color = tint),
        )
    }
}

@Composable
private fun ExpiredLedgerRow(
    name: String,
    plans: AnnotatedString,
    swatch: Color,
    tabLabel: String,
    onLogInAgain: () -> Unit,
) {
    Column(Modifier.padding(top = 13.dp, bottom = 15.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformSwatch(color = swatch)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(text = name, style = BroadsheetText.ledgerName)
                Text(text = plans, style = BroadsheetText.ledgerPlans)
            }
            Row(
                modifier = Modifier.heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning_circle),
                    contentDescription = null,
                    tint = Destructive,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.ledger_status_expired).uppercase(),
                    style = BroadsheetText.ledgerStatus.copy(color = Destructive),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .heightIn(min = 46.dp)
                .background(Destructive)
                .clickable(role = Role.Button, onClick = onLogInAgain),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_u_up_left),
                contentDescription = null,
                tint = Paper,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
            )
            Text(
                text = stringResource(R.string.ledger_log_in_again),
                style = BroadsheetText.getStarted.copy(fontSize = 15.sp),
            )
        }
        Text(
            text = stringResource(R.string.ledger_log_in_again_caption, tabLabel),
            style = BroadsheetText.ledgerPlans.copy(lineHeight = (11 * 1.55).sp),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun connectedPlans(account: Account?, platform: Platform): AnnotatedString {
    val plan = shortPlan(account?.planLine ?: platform.seedPlans)
    val work = account?.kind == AccountKind.WORK
    val kind = if (work) {
        stringResource(R.string.ledger_kind_work)
    } else {
        stringResource(R.string.ledger_kind_personal)
    }
    val place = if (account?.needsReauth == true) {
        stringResource(R.string.ledger_expired_on_tabs)
    } else {
        stringResource(R.string.ledger_active_on_tabs)
    }
    return buildAnnotatedString {
        append(plan)
        append(" · ")
        if (work) {
            withStyle(
                SpanStyle(
                    color = WorkMarkGold,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = SourceSerif4,
                ),
            ) {
                append(kind)
            }
        } else {
            append(kind)
        }
        append(" · ")
        append(place)
    }
}

internal fun shortPlan(planLine: String): String =
    planLine.substringBefore('\n').substringBefore(" · ").trim()

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun LedgerScreenPreview() {
    AISUMTheme {
        LedgerScreen(
            accounts = emptyList(),
            onAddPlatform = { _, _ -> },
            onAdd = {},
            onTab = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun LedgerScreenConnectedPreview() {
    AISUMTheme {
        LedgerScreen(
            accounts = listOf(
                fakeClaudeAccount(1, AccountKind.PERSONAL),
                fakeCursorAccount(2, AccountKind.PERSONAL),
                fakeM365Account(3, AccountKind.WORK),
            ),
            onAddPlatform = { _, _ -> },
            onAdd = {},
            onTab = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun LedgerScreenExpiredPreview() {
    AISUMTheme {
        LedgerScreen(
            accounts = listOf(
                fakeClaudeAccount(1, AccountKind.PERSONAL).copy(needsReauth = true),
                fakeCursorAccount(2, AccountKind.PERSONAL),
                fakeM365Account(3, AccountKind.WORK),
            ),
            onAddPlatform = { _, _ -> },
            onAdd = {},
            onTab = {},
        )
    }
}
