package com.ericmbpeck.ai_sum.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Destructive
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Neutral500
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.SourceSerif4

@Composable
fun RemoveConfirmOverlay(
    platformName: String,
    emailLine: String,
    hasSiblingSamePlatform: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.42f))
                .clickable(role = Role.Button, onClick = onCancel),
        )
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .background(Paper)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Destructive),
            )
            Column(Modifier.padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 18.dp)) {
                Text(
                    text = stringResource(R.string.remove_title),
                    style = BroadsheetText.removeTitle,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = SourceSerif4,
                                fontWeight = FontWeight.SemiBold,
                                color = Ink,
                            ),
                        ) {
                            append(platformName)
                            append(" · ")
                            append(emailLine)
                        }
                        append(stringResource(R.string.remove_body_rest))
                        if (hasSiblingSamePlatform) {
                            append(stringResource(R.string.remove_other_untouched, platformName))
                        }
                    },
                    style = BroadsheetText.removeBody,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .border(1.dp, Neutral500)
                            .clickable(role = Role.Button, onClick = onCancel),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = BroadsheetText.removeAction,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .background(Destructive)
                            .clickable(role = Role.Button, onClick = onConfirm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.action_remove),
                            style = BroadsheetText.removeAction.copy(color = Paper),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.remove_caption),
                    style = BroadsheetText.removeCaption,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun RemoveConfirmPreview() {
    AISUMTheme {
        Box(Modifier.background(Paper)) {
            RemoveConfirmOverlay(
                platformName = "Claude",
                emailLine = "alex@work.example.com",
                hasSiblingSamePlatform = true,
                onCancel = {},
                onConfirm = {},
            )
        }
    }
}
