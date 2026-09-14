package com.ericmbpeck.ai_sum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.ui.theme.Accent700
import com.ericmbpeck.ai_sum.ui.theme.BreachFigure
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Destructive
import com.ericmbpeck.ai_sum.ui.theme.Divider
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral200
import com.ericmbpeck.ai_sum.ui.theme.Neutral500
import com.ericmbpeck.ai_sum.ui.theme.Neutral600
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold

data class TabItem(
    val accountId: Long,
    val label: String,
    val barColor: Color,
    val figureColor: Color,
    val kind: AccountKind,
    val selected: Boolean,
    val expired: Boolean = false,
)

@Composable
fun PlatformTabStrip(
    tabs: List<TabItem>,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    centeredAdd: Boolean = false,
    finishHint: Boolean = false,
    settingsMode: Boolean = false,
    onRemove: () -> Unit = {},
    onTab: (Long) -> Unit = {},
    addSelected: Boolean = false,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Paper)
            .padding(bottom = 6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Ink),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Ink),
        )
        when {
            centeredAdd && tabs.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onAdd)
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus_circle),
                            contentDescription = stringResource(R.string.cd_add),
                            tint = Accent700,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_add),
                            style = BroadsheetText.addLabel.copy(fontSize = BroadsheetText.tabEmptyHint.fontSize),
                        )
                    }
                }
            }
            finishHint && emptyMessage != null && tabs.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = emptyMessage, style = BroadsheetText.tabEmptyHint)
                }
            }
            emptyMessage != null && tabs.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus_circle),
                        contentDescription = null,
                        tint = Neutral600,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = emptyMessage, style = BroadsheetText.tabEmptyHint)
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val scroll = rememberScrollState()
                    Column(Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(scroll)
                                .padding(horizontal = 8.dp),
                        ) {
                            tabs.forEach { tab ->
                                TabCell(tab = tab, onClick = { onTab(tab.accountId) })
                            }
                        }
                        Row(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 2.dp),
                        ) {
                            val thumb = if (scroll.maxValue == 0) 1f else scroll.value.toFloat() / scroll.maxValue
                            Box(
                                Modifier
                                    .height(2.dp)
                                    .weight((0.4f + thumb).coerceAtMost(2f))
                                    .background(Neutral500),
                            )
                            Spacer(Modifier.width(2.dp))
                            Box(
                                Modifier
                                    .height(2.dp)
                                    .weight((1.4f - thumb).coerceAtLeast(0.4f))
                                    .background(Neutral200),
                            )
                        }
                    }
                    Box(
                        Modifier
                            .padding(top = 6.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
                            .width(1.dp)
                            .height(42.dp)
                            .background(Divider),
                    )
                    Column(
                        modifier = Modifier
                            .width(54.dp)
                            .height(56.dp)
                            .clickable(
                                role = Role.Button,
                                onClick = if (settingsMode) onRemove else onAdd,
                            )
                            .padding(top = 9.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (settingsMode) R.drawable.ic_minus_circle else R.drawable.ic_plus_circle,
                            ),
                            contentDescription = stringResource(
                                if (settingsMode) R.string.cd_remove else R.string.cd_add,
                            ),
                            tint = if (settingsMode) BreachFigure else Accent700,
                            modifier = Modifier.size(21.dp),
                        )
                        Text(
                            text = stringResource(
                                if (settingsMode) R.string.action_remove else R.string.action_add,
                            ),
                            style = BroadsheetText.addLabel.copy(
                                color = if (settingsMode) BreachFigure else Accent700,
                            ),
                        )
                        Box(
                            Modifier
                                .width(18.dp)
                                .height(2.dp)
                                .background(
                                    if (addSelected && !settingsMode) Accent700 else Color.Transparent,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCell(tab: TabItem, onClick: () -> Unit) {
    val labelColor = when {
        tab.expired -> Destructive
        tab.selected -> tab.figureColor
        else -> Neutral600
    }
    Column(
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 74.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, top = 9.dp, end = 8.dp, bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tab.expired) {
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .size(6.dp)
                        .background(Destructive),
                )
            }
            if (tab.kind == AccountKind.WORK) {
                Text(
                    text = stringResource(R.string.work_mark),
                    style = BroadsheetText.workTick.copy(color = WorkMarkGold),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Text(
                text = tab.label,
                style = BroadsheetText.tabLabel.copy(color = labelColor),
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .width(18.dp)
                .height(2.dp)
                .background(
                    when {
                        tab.selected && tab.expired -> Destructive
                        tab.selected -> tab.barColor
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
fun WorkTick(
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) WorkMarkGold else Neutral600
    Row(
        modifier = modifier
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(17.dp)
                .then(
                    if (selected) {
                        Modifier.background(WorkMarkGold)
                    } else {
                        Modifier.border(1.5.dp, Neutral500)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Paper),
                )
            }
        }
        Text(
            text = stringResource(R.string.work_tick).uppercase(),
            style = BroadsheetText.workTick.copy(color = color),
        )
    }
}
