package com.ericmbpeck.ai_sum.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.ui.components.PlatformTabStrip
import com.ericmbpeck.ai_sum.ui.components.ScreenHeader
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.Accent700
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Paper

@Composable
fun EmptyScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = stringResource(R.string.screen_title_usage),
            meta = stringResource(R.string.empty_meta),
            onGear = {},
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(top = 74.dp)
                .widthIn(max = 300.dp),
        ) {
            Text(
                text = stringResource(R.string.empty_headline),
                style = BroadsheetText.emptyHeadline,
            )
            Text(
                text = stringResource(R.string.empty_body),
                style = BroadsheetText.emptyBody,
                modifier = Modifier.padding(top = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp)
                    .height(50.dp)
                    .background(Accent700)
                    .clickable(role = Role.Button, onClick = onGetStarted),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus_circle),
                    contentDescription = null,
                    tint = Paper,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.empty_get_started),
                    style = BroadsheetText.getStarted,
                )
            }
            Text(
                text = stringResource(R.string.empty_caption),
                style = BroadsheetText.footnote,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        PlatformTabStrip(
            tabs = emptyList(),
            onAdd = onGetStarted,
            centeredAdd = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun EmptyScreenPreview() {
    AISUMTheme { EmptyScreen(onGetStarted = {}) }
}
