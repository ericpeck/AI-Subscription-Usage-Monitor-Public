package com.ericmbpeck.ai_sum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.CursorFigure
import com.ericmbpeck.ai_sum.ui.theme.Divider
import com.ericmbpeck.ai_sum.ui.theme.Neutral600
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold

@Composable
fun ScreenHeader(
    title: String,
    meta: String,
    modifier: Modifier = Modifier,
    work: Boolean = false,
    titleSuffix: String? = null,
    refreshTint: Color? = null,
    onRefresh: (() -> Unit)? = null,
    onInfo: (() -> Unit)? = null,
    showInfo: Boolean = false,
    gearTint: Color = Neutral600,
    onGear: (() -> Unit)? = null,
    metaColor: Color = Neutral600,
    ruleColor: Color = Divider,
    ruleHeight: Dp = 1.dp,
    statusIcon: Int? = null,
    statusTint: Color = Neutral600,
    onStatus: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 14.dp, end = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (work) {
                Text(
                    text = stringResource(R.string.work_mark),
                    style = BroadsheetText.workTick.copy(color = WorkMarkGold),
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = BroadsheetText.screenTitle,
                )
                if (titleSuffix != null) {
                    Text(
                        text = titleSuffix,
                        style = BroadsheetText.titleSuffix,
                    )
                }
            }
            if (onRefresh != null && refreshTint != null) {
                HeaderIcon(
                    drawable = R.drawable.ic_arrows_clockwise,
                    contentDescription = stringResource(R.string.cd_refresh),
                    tint = refreshTint,
                    onClick = onRefresh,
                )
            }
            if (showInfo) {
                HeaderIcon(
                    drawable = R.drawable.ic_info,
                    contentDescription = stringResource(R.string.cd_info),
                    tint = Neutral600,
                    onClick = onInfo,
                )
            }
            if (statusIcon != null) {
                HeaderIcon(
                    drawable = statusIcon,
                    contentDescription = stringResource(R.string.cd_signed_out),
                    tint = statusTint,
                    onClick = onStatus,
                )
            }
            if (onGear != null) {
                HeaderIcon(
                    drawable = R.drawable.ic_gear_six,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = gearTint,
                    onClick = onGear,
                )
            }
        }
        Text(
            text = meta.uppercase(),
            style = BroadsheetText.screenMeta.copy(color = metaColor),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(ruleHeight)
                .background(ruleColor),
        )
    }
}

@Composable
private fun HeaderIcon(
    drawable: Int,
    contentDescription: String,
    tint: Color,
    onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        indication = null,
                        interactionSource = interaction,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
fun SectionRule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider),
    )
}

@Composable
fun PlatformSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .background(color),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF3F2F2, widthDp = 412)
@Composable
private fun ScreenHeaderPreview() {
    AISUMTheme {
        Column(Modifier.background(Paper)) {
            ScreenHeader(
                title = "Cursor",
                meta = "Personal account · updated 2 min ago",
                refreshTint = CursorFigure,
                onRefresh = {},
                onGear = {},
            )
        }
    }
}
