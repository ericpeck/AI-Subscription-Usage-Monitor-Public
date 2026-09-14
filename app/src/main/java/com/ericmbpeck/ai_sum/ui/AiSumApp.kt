package com.ericmbpeck.ai_sum.ui

import android.content.Intent
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ericmbpeck.ai_sum.R
import com.ericmbpeck.ai_sum.data.FakeAccountStore
import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.ui.info.InfoScreen
import com.ericmbpeck.ai_sum.ui.onboarding.EmptyScreen
import com.ericmbpeck.ai_sum.ui.onboarding.LedgerScreen
import com.ericmbpeck.ai_sum.ui.onboarding.SignInScreen
import com.ericmbpeck.ai_sum.ui.onboarding.SignInUiState
import com.ericmbpeck.ai_sum.ui.platform.PlatformScreen
import com.ericmbpeck.ai_sum.ui.platform.PlatformUiState
import com.ericmbpeck.ai_sum.usage.ClaudeWebView
import com.ericmbpeck.ai_sum.usage.LiveSignIn
import com.ericmbpeck.ai_sum.usage.UsageReadResult
import com.ericmbpeck.ai_sum.usage.signInReadTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object EmptyRoute

@Serializable
data object LedgerRoute

@Serializable
data class PlatformRoute(val accountId: Long)

@Serializable
data class SignInRoute(
    val platformId: String,
    val work: Boolean,
    val accountId: Long = -1L,
)

@Serializable
data object InfoRoute

@Composable
fun AiSumApp() {
    val accounts by FakeAccountStore.accounts.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val start = if (accounts.isEmpty()) EmptyRoute else PlatformRoute(accounts.first().id)
    var refreshKey by remember { mutableIntStateOf(0) }
    var settingsMode by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun openLedger() {
        settingsMode = false
        showRemoveConfirm = false
        navController.navigate(LedgerRoute) {
            launchSingleTop = true
        }
    }

    fun openPlatform(accountId: Long) {
        navController.navigate(PlatformRoute(accountId)) {
            popUpTo(EmptyRoute) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun openEmpty() {
        settingsMode = false
        showRemoveConfirm = false
        navController.navigate(EmptyRoute) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun openSignIn(account: Account) {
        navController.navigate(
            SignInRoute(
                platformId = account.platformId,
                work = account.kind == AccountKind.WORK,
                accountId = account.id,
            ),
        )
    }

    fun applyPlatformLiveRead(account: Account, result: UsageReadResult) {
        when (result) {
            is UsageReadResult.Success -> {
                LiveSignIn.persistCookies()
                when (account.platformId) {
                    Platforms.CURSOR -> {
                        FakeAccountStore.updateLive(
                            accountId = account.id,
                            planLine = result.planLine,
                            windows = result.windows,
                        )
                        result.grokBot?.let { FakeAccountStore.rememberGrokBotFromCursor(it) }
                    }
                    Platforms.GROK_BOT -> {
                        val grokBot = result.grokBot
                        if (grokBot != null) {
                            FakeAccountStore.rememberGrokBotFromCursor(grokBot)
                            FakeAccountStore.updateLive(
                                accountId = account.id,
                                planLine = grokBot.planLine,
                                windows = grokBot.windows,
                            )
                        }
                        FakeAccountStore.accountFor(Platforms.CURSOR)?.let { cursor ->
                            FakeAccountStore.updateLive(
                                accountId = cursor.id,
                                planLine = result.planLine,
                                windows = result.windows,
                            )
                        }
                    }
                    else -> {
                        FakeAccountStore.updateLive(
                            accountId = account.id,
                            planLine = result.planLine,
                            windows = result.windows,
                        )
                    }
                }
                FakeAccountStore.clearLiveReauth(account.platformId)
                refreshKey += 1
            }
            UsageReadResult.NeedsSignIn -> {
                val reauthPlatform = if (account.platformId == Platforms.GROK_BOT) {
                    Platforms.CURSOR
                } else {
                    account.platformId
                }
                FakeAccountStore.markLiveNeedsReauth(reauthPlatform)
            }
            UsageReadResult.FreePlan,
            is UsageReadResult.Failed,
            -> Unit
        }
    }

    fun confirmRemove(account: Account) {
        showRemoveConfirm = false
        val platformId = account.platformId
        val wasLiveWeb = account.liveRead && LiveSignIn.usesInAppBrowser(platformId)
        val next = FakeAccountStore.remove(account.id)
        if (wasLiveWeb) {
            LiveSignIn.forgetAfterRemove(
                removedPlatformId = platformId,
                remainingHasLiveWeb = FakeAccountStore.hasLiveWebSession(),
            )
        }
        if (next == null) {
            openEmpty()
        } else {
            openPlatform(next.id)
        }
    }

    NavHost(
        navController = navController,
        startDestination = start,
    ) {
        composable<EmptyRoute> {
            EmptyScreen(onGetStarted = { openLedger() })
        }
        composable<LedgerRoute> {
            LedgerScreen(
                accounts = accounts,
                onAddPlatform = { platformId, work ->
                    val existing = FakeAccountStore.accountFor(platformId)
                    if (existing != null) {
                        settingsMode = false
                        showRemoveConfirm = false
                        if (existing.needsReauth) {
                            openPlatform(existing.id)
                        }
                    } else if (LiveSignIn.usesInAppBrowser(platformId)) {
                        navController.navigate(SignInRoute(platformId, work))
                    } else if (platformId == Platforms.GROK_BOT) {
                        val live = FakeAccountStore.addGrokBotFromCursor(work)
                        val account = live ?: FakeAccountStore.add(platformId, work)
                        settingsMode = false
                        showRemoveConfirm = false
                        openPlatform(account.id)
                    } else {
                        val account = FakeAccountStore.add(platformId, work)
                        settingsMode = false
                        showRemoveConfirm = false
                        openPlatform(account.id)
                    }
                },
                onAdd = { openLedger() },
                onTab = { accountId ->
                    settingsMode = false
                    showRemoveConfirm = false
                    openPlatform(accountId)
                },
            )
        }
        composable<SignInRoute> { entry ->
            val route = entry.toRoute<SignInRoute>()
            val platform = Platforms.byId(route.platformId)
            val reader = LiveSignIn.reader(route.platformId)
            var uiState by remember { mutableStateOf<SignInUiState>(SignInUiState.Idle) }
            var webView by remember { mutableStateOf<WebView?>(null) }
            var currentUrl by remember { mutableStateOf(reader.signInUrl) }
            var lastAttemptedUrl by remember { mutableStateOf<String?>(null) }
            var autoReadJob by remember { mutableStateOf<Job?>(null) }
            var acceptedSession by remember { mutableStateOf(false) }
            val freeText = when (route.platformId) {
                Platforms.GROK -> stringResource(R.string.grok_free_disclaimer)
                Platforms.CURSOR -> stringResource(R.string.cursor_free_disclaimer)
                Platforms.CODEX -> stringResource(R.string.codex_free_disclaimer)
                else -> stringResource(R.string.claude_free_disclaimer)
            }
            val needSession = stringResource(R.string.sign_in_need_session, reader.hostLabel)
            val readFailed = stringResource(R.string.sign_in_read_failed)

            fun applyRead(
                result: UsageReadResult,
                silentIfNeedsSignIn: Boolean,
            ) {
                when (result) {
                    is UsageReadResult.Success -> {
                        if (acceptedSession) return
                        val grokBot = result.grokBot
                        if (route.platformId == Platforms.GROK_BOT && grokBot == null) {
                            uiState = SignInUiState.Message(readFailed)
                            return
                        }
                        acceptedSession = true
                        LiveSignIn.persistCookies()
                        val livePlan = grokBot?.planLine?.takeIf {
                            route.platformId == Platforms.GROK_BOT
                        } ?: result.planLine
                        val liveWindows = grokBot?.windows?.takeIf {
                            route.platformId == Platforms.GROK_BOT
                        } ?: result.windows
                        if (route.platformId == Platforms.CURSOR ||
                            route.platformId == Platforms.GROK_BOT
                        ) {
                            grokBot?.let { FakeAccountStore.rememberGrokBotFromCursor(it) }
                        }
                        if (route.platformId == Platforms.GROK_BOT) {
                            FakeAccountStore.accountFor(Platforms.CURSOR)?.let { cursor ->
                                FakeAccountStore.updateLive(
                                    accountId = cursor.id,
                                    planLine = result.planLine,
                                    windows = result.windows,
                                )
                            }
                        }
                        val accountId = if (route.accountId >= 0L) {
                            FakeAccountStore.updateLive(
                                accountId = route.accountId,
                                planLine = livePlan,
                                windows = liveWindows,
                            )
                            FakeAccountStore.clearLiveReauth(route.platformId)
                            route.accountId
                        } else {
                            val created = FakeAccountStore.addLive(
                                platformId = route.platformId,
                                work = route.work,
                                planLine = livePlan,
                                footNote = LiveSignIn.footnote(route.platformId),
                                windows = liveWindows,
                            )
                            FakeAccountStore.clearLiveReauth(route.platformId)
                            created.id
                        }
                        uiState = SignInUiState.Idle
                        if (route.accountId >= 0L && navController.popBackStack()) {
                            return
                        }
                        openPlatform(accountId)
                    }
                    UsageReadResult.FreePlan -> {
                        uiState = SignInUiState.Message(freeText)
                    }
                    UsageReadResult.NeedsSignIn -> {
                        if (!silentIfNeedsSignIn) {
                            lastAttemptedUrl = null
                        }
                        uiState = if (silentIfNeedsSignIn) {
                            SignInUiState.Idle
                        } else {
                            SignInUiState.Message(needSession)
                        }
                    }
                    is UsageReadResult.Failed -> {
                        if (!silentIfNeedsSignIn) {
                            lastAttemptedUrl = null
                        }
                        uiState = if (silentIfNeedsSignIn) {
                            SignInUiState.Idle
                        } else {
                            SignInUiState.Message(readFailed)
                        }
                    }
                }
            }

            fun readUsage(silentIfNeedsSignIn: Boolean) {
                val view = webView ?: return
                if (acceptedSession) return
                if (uiState is SignInUiState.Reading) return
                scope.launch {
                    uiState = SignInUiState.Reading
                    var target = view.signInReadTarget()
                    var result = reader.read(target)
                    if (result is UsageReadResult.NeedsSignIn) {
                        val extra = if (silentIfNeedsSignIn) 2 else 1
                        var attempt = 0
                        while (result is UsageReadResult.NeedsSignIn && attempt < extra) {
                            attempt += 1
                            delay(1_200)
                            if (acceptedSession) return@launch
                            target = view.signInReadTarget()
                            val readView = when {
                                LiveSignIn.isProductHost(route.platformId, target.url) -> target
                                LiveSignIn.isProductHost(route.platformId, view.url) -> view
                                else -> break
                            }
                            currentUrl = readView.url.orEmpty()
                            result = reader.read(readView)
                        }
                    }
                    applyRead(result, silentIfNeedsSignIn)
                }
            }

            fun scheduleAutoRead(url: String, previousUrl: String? = null) {
                if (acceptedSession) return
                if (!LiveSignIn.shouldAttemptUsageRead(route.platformId, url, previousUrl)) {
                    lastAttemptedUrl = null
                    return
                }
                if (url == lastAttemptedUrl) return
                if (uiState is SignInUiState.Reading) return
                lastAttemptedUrl = url
                if (uiState is SignInUiState.Message) {
                    uiState = SignInUiState.Idle
                }
                autoReadJob?.cancel()
                autoReadJob = scope.launch {
                    delay(800)
                    readUsage(silentIfNeedsSignIn = true)
                }
            }

            SignInScreen(
                platform = platform,
                uiState = uiState,
                currentUrl = currentUrl,
                work = route.work,
                onBack = { navController.popBackStack() },
                onContinue = {
                    autoReadJob?.cancel()
                    readUsage(silentIfNeedsSignIn = false)
                },
                onWebViewReady = { webView = it },
                onUrlChanged = { url ->
                    val previous = currentUrl
                    currentUrl = url
                    scheduleAutoRead(url, previous)
                },
                onPopupClosed = {
                    lastAttemptedUrl = null
                    autoReadJob?.cancel()
                    autoReadJob = scope.launch {
                        delay(800)
                        val target = webView?.signInReadTarget()
                        val first = target?.url.orEmpty()
                        val fromPopup = currentUrl
                        currentUrl = first
                        if (LiveSignIn.shouldAttemptUsageRead(route.platformId, first, fromPopup)) {
                            lastAttemptedUrl = first
                            readUsage(silentIfNeedsSignIn = true)
                            return@launch
                        }
                        delay(1_200)
                        lastAttemptedUrl = null
                        val laterTarget = webView?.signInReadTarget()
                        val later = laterTarget?.url.orEmpty()
                        val previous = currentUrl
                        currentUrl = later
                        scheduleAutoRead(later, previous)
                    }
                },
            )
        }
        composable<InfoRoute> {
            val context = LocalContext.current
            val shareSubject = stringResource(R.string.info_share_subject)
            val shareBody = stringResource(R.string.info_what_body)
            InfoScreen(
                onBack = { navController.popBackStack() },
                onDonate = {},
                onShare = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                        putExtra(Intent.EXTRA_TEXT, shareBody)
                    }
                    context.startActivity(
                        Intent.createChooser(send, context.getString(R.string.action_share)),
                    )
                },
            )
        }
        composable<PlatformRoute> { entry ->
            val route = entry.toRoute<PlatformRoute>()
            val account = accounts.firstOrNull { it.id == route.accountId }
                ?: FakeAccountStore.account(route.accountId)
            if (account != null) {
                var liveWebView by remember(account.id) { mutableStateOf<WebView?>(null) }
                var openedReadDone by remember(account.id) { mutableStateOf(false) }
                val cursorLive = accounts.any {
                    it.platformId == Platforms.CURSOR && it.liveRead && !it.needsReauth
                }
                val liveReader = when {
                    account.needsReauth -> null
                    account.liveRead -> LiveSignIn.reader(account.platformId)
                    account.platformId == Platforms.GROK_BOT && cursorLive ->
                        LiveSignIn.reader(Platforms.GROK_BOT)
                    else -> null
                }
                LaunchedEffect(account.id, liveWebView) {
                    val view = liveWebView ?: return@LaunchedEffect
                    val reader = liveReader ?: return@LaunchedEffect
                    if (account.needsReauth || openedReadDone) {
                        return@LaunchedEffect
                    }
                    openedReadDone = true
                    applyPlatformLiveRead(account, reader.read(view))
                }
                Box {
                    PlatformScreen(
                        uiState = PlatformUiState(
                            account = account,
                            platform = Platforms.byId(account.platformId),
                            tabs = accounts,
                        ),
                        animationKey = refreshKey,
                        settingsMode = settingsMode,
                        showRemoveConfirm = showRemoveConfirm,
                        onRefresh = {
                            if (liveReader == null) {
                                refreshKey += 1
                                return@PlatformScreen
                            }
                            val view = liveWebView ?: return@PlatformScreen
                            val reader = liveReader
                            scope.launch {
                                applyPlatformLiveRead(account, reader.read(view))
                            }
                        },
                        onAdd = { openLedger() },
                        onTab = { accountId ->
                            showRemoveConfirm = false
                            openPlatform(accountId)
                        },
                        onReauth = { openSignIn(account) },
                        onToggleSettings = {
                            showRemoveConfirm = false
                            settingsMode = !settingsMode
                        },
                        onToggleNotify = {
                            FakeAccountStore.setNotifyOnReset(
                                account.id,
                                !account.notifyOnReset,
                            )
                        },
                        onRemoveRequest = { showRemoveConfirm = true },
                        onDismissRemove = { showRemoveConfirm = false },
                        onConfirmRemove = { confirmRemove(account) },
                        onInfo = {
                            navController.navigate(InfoRoute) {
                                launchSingleTop = true
                            }
                        },
                    )
                    if (liveReader != null && !account.needsReauth) {
                        ClaudeWebView(
                            startUrl = liveReader.readUrl,
                            onReady = { liveWebView = it },
                            modifier = Modifier
                                .size(1.dp)
                                .align(Alignment.TopStart),
                        )
                    }
                }
            }
        }
    }
}
