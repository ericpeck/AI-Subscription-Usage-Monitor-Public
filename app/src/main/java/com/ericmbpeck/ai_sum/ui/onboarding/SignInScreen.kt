package com.ericmbpeck.ai_sum.ui.onboarding

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.model.Platform
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.ui.theme.AISUMTheme
import com.ericmbpeck.ai_sum.ui.theme.Accent700
import com.ericmbpeck.ai_sum.ui.theme.BroadsheetText
import com.ericmbpeck.ai_sum.ui.theme.Divider
import com.ericmbpeck.ai_sum.ui.theme.Ink
import com.ericmbpeck.ai_sum.ui.theme.Paper
import com.ericmbpeck.ai_sum.ui.theme.WorkMarkGold
import com.ericmbpeck.ai_sum.usage.ClaudeWebView
import com.ericmbpeck.ai_sum.usage.LiveSignIn
import com.ericmbpeck.ai_sum.usage.handleSignInBack

sealed interface SignInUiState {
    data object Idle : SignInUiState
    data object Reading : SignInUiState
    data class Message(val text: String) : SignInUiState
}

@Composable
fun SignInScreen(
    platform: Platform,
    uiState: SignInUiState,
    currentUrl: String,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onPopupClosed: () -> Unit,
    modifier: Modifier = Modifier,
    work: Boolean = false,
) {
    val statusText = when (uiState) {
        SignInUiState.Idle -> null
        SignInUiState.Reading -> null
        is SignInUiState.Message -> uiState.text
    }
    val canContinue = LiveSignIn.isProductHost(platform.id, currentUrl) &&
        uiState !is SignInUiState.Reading
    var webView by remember { mutableStateOf<WebView?>(null) }
    val overlayInteraction = remember { MutableInteractionSource() }
    val conceal = uiState is SignInUiState.Reading ||
        (
            LiveSignIn.concealProductHost(platform.id) &&
                LiveSignIn.shouldAttemptUsageRead(platform.id, currentUrl)
            )
    val disclaimerShort = if (platform.id == Platforms.CLAUDE) {
        stringResource(R.string.claude_free_disclaimer_short)
    } else {
        null
    }

    BackHandler {
        webView?.handleSignInBack(
            onExit = onBack,
            onPopupDismissed = onPopupClosed,
        ) ?: onBack()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        SignInBrowserChrome(
            platform = platform,
            currentHost = LiveSignIn.displayHost(platform.id, currentUrl),
            canContinue = canContinue,
            reading = uiState is SignInUiState.Reading,
            work = work,
            disclaimerShort = disclaimerShort,
            onBack = {
                webView?.handleSignInBack(
                    onExit = onBack,
                    onPopupDismissed = onPopupClosed,
                ) ?: onBack()
            },
            onContinue = onContinue,
        )
        if (statusText != null) {
            Text(
                text = statusText,
                style = BroadsheetText.footnote.copy(color = Ink),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Divider),
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ClaudeWebView(
                startUrl = LiveSignIn.reader(platform.id).signInUrl,
                onReady = { view ->
                    webView = view
                    onWebViewReady(view)
                },
                onUrlChanged = onUrlChanged,
                onPopupClosed = onPopupClosed,
                allowPopups = true,
                beforeFirstLoad = { LiveSignIn.forgetPlatform(platform.id) },
                modifier = Modifier.fillMaxSize(),
            )
            if (conceal) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Paper)
                        .clickable(
                            indication = null,
                            interactionSource = overlayInteraction,
                            onClick = {},
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.sign_in_reading),
                        style = BroadsheetText.planLine,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignInBrowserChrome(
    platform: Platform,
    currentHost: String,
    canContinue: Boolean,
    reading: Boolean,
    work: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    disclaimerShort: String? = null,
) {
    val backInteraction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
                    text = stringResource(R.string.action_add).uppercase(),
                    style = BroadsheetText.screenMeta.copy(
                        color = Accent700,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.07.em,
                    ),
                )
            }
            if (work) {
                Text(
                    text = stringResource(R.string.work_mark),
                    style = BroadsheetText.workTick.copy(color = WorkMarkGold),
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_lock_simple),
                contentDescription = stringResource(R.string.cd_in_app_browser),
                tint = Accent700,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(14.dp),
            )
            Text(
                text = currentHost,
                style = BroadsheetText.signInChrome,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp),
            )
            if (reading) {
                Text(
                    text = stringResource(R.string.sign_in_reading),
                    style = BroadsheetText.signInChrome,
                )
            } else if (canContinue) {
                Text(
                    text = stringResource(R.string.sign_in_continue, platform.displayName),
                    style = BroadsheetText.signInContinue.copy(color = Accent700),
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onContinue)
                        .padding(vertical = 10.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.sign_in_meta).uppercase(),
            style = BroadsheetText.screenMeta,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (disclaimerShort != null) {
            Text(
                text = disclaimerShort,
                style = BroadsheetText.footnote,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun SignInScreenPreview() {
    AISUMTheme {
        SignInScreen(
            platform = Platforms.byId(Platforms.CLAUDE),
            uiState = SignInUiState.Idle,
            currentUrl = LiveSignIn.reader(Platforms.CLAUDE).signInUrl,
            onBack = {},
            onContinue = {},
            onWebViewReady = {},
            onUrlChanged = {},
            onPopupClosed = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 740)
@Composable
private fun SignInScreenWorkPreview() {
    AISUMTheme {
        SignInScreen(
            platform = Platforms.byId(Platforms.CLAUDE),
            uiState = SignInUiState.Idle,
            currentUrl = LiveSignIn.reader(Platforms.CLAUDE).signInUrl,
            work = true,
            onBack = {},
            onContinue = {},
            onWebViewReady = {},
            onUrlChanged = {},
            onPopupClosed = {},
        )
    }
}
