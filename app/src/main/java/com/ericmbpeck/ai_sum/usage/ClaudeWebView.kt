package com.ericmbpeck.ai_sum.usage

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
fun WebView.configureClaudeSession(chromeClient: WebChromeClient? = null) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.userAgentString = ClaudeHosts.chromeLikeUserAgent(settings.userAgentString)
    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_YES
    val bridge = ClaudeJsBridge()
    addJavascriptInterface(bridge, CLAUDE_BRIDGE_NAME)
    addJavascriptInterface(bridge, USAGE_BRIDGE_NAME)
    tag = bridge
    webViewClient = ClaudeWebViewClient()
    if (chromeClient != null) {
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        webChromeClient = chromeClient
    }
}

class ClaudeWebViewClient : WebViewClient() {
    var onUrlChanged: ((String) -> Unit)? = null
    var pageFinishedWaiter: ((String) -> Unit)? = null

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean {
        val scheme = request.url.scheme.orEmpty()
        // Stay in this CookieManager. Do not hand Google / X / Claude off to Chrome.
        return scheme != "http" && scheme != "https"
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        onUrlChanged?.invoke(url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        pageFinishedWaiter?.invoke(url)
        onUrlChanged?.invoke(url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        onUrlChanged?.invoke(url)
    }
}

/**
 * Google's "Continue with Google" opens a real window. Folding that into the
 * parent WebView drops `window.opener` and leaves the session stuck. A child
 * WebView in the same [CookieManager] is the in-app equivalent of a tab.
 */
class ClaudeWebChromeClient(
    private val container: FrameLayout,
    private val main: WebView,
) : WebChromeClient() {
    var onPopupClosed: (() -> Unit)? = null
    var onUrlChanged: ((String) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val popup = WebView(view.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            configureClaudeSession(chromeClient = this@ClaudeWebChromeClient)
            settings.userAgentString = main.settings.userAgentString
            (webViewClient as ClaudeWebViewClient).onUrlChanged = { url ->
                onUrlChanged?.invoke(url)
            }
        }
        container.addView(popup)
        val transport = resultMsg.obj as WebViewTransport
        transport.webView = popup
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(window: WebView) {
        if (window === main) return
        container.removeView(window)
        window.destroy()
        onPopupClosed?.invoke()
    }
}

/**
 * Prefer a product-origin child window (Google / X OAuth popup that redirected
 * back) so `fetch` runs where the signed-in document actually is.
 */
fun WebView.signInReadTarget(): WebView {
    val container = parent as? ViewGroup ?: return this
    for (i in container.childCount - 1 downTo 0) {
        val child = container.getChildAt(i)
        if (child is WebView &&
            LiveSignIn.isAnyProductHost(child.url) &&
            child.tag is ClaudeJsBridge
        ) {
            return child
        }
    }
    return this
}

fun WebView.handleSignInBack(
    onExit: () -> Unit,
    onPopupDismissed: () -> Unit = {},
) {
    val container = parent as? ViewGroup
    if (container != null && container.childCount > 1) {
        val top = container.getChildAt(container.childCount - 1)
        if (top is WebView && top !== this) {
            if (top.canGoBack()) {
                top.goBack()
            } else {
                container.removeView(top)
                top.destroy()
                onPopupDismissed()
            }
            return
        }
    }
    if (canGoBack()) goBack() else onExit()
}

@Composable
fun ClaudeWebView(
    startUrl: String,
    onReady: (WebView) -> Unit,
    modifier: Modifier = Modifier,
    onUrlChanged: ((String) -> Unit)? = null,
    onPopupClosed: (() -> Unit)? = null,
    allowPopups: Boolean = false,
    beforeFirstLoad: (() -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val frame = FrameLayout(context)
            val main = WebView(context)
            val chrome = if (allowPopups) {
                ClaudeWebChromeClient(frame, main)
            } else {
                null
            }
            main.configureClaudeSession(chromeClient = chrome)
            (main.webViewClient as ClaudeWebViewClient).onUrlChanged = onUrlChanged
            chrome?.onUrlChanged = onUrlChanged
            chrome?.onPopupClosed = onPopupClosed
            frame.addView(
                main,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            beforeFirstLoad?.invoke()
            main.loadUrl(startUrl)
            onReady(main)
            frame
        },
        update = { frame ->
            val main = frame.getChildAt(0) as? WebView ?: return@AndroidView
            (main.webViewClient as? ClaudeWebViewClient)?.onUrlChanged = onUrlChanged
            val chrome = main.webChromeClient as? ClaudeWebChromeClient
            if (chrome != null) {
                chrome.onUrlChanged = onUrlChanged
                chrome.onPopupClosed = onPopupClosed
            }
        },
        onRelease = { frame ->
            for (i in frame.childCount - 1 downTo 0) {
                val child = frame.getChildAt(i)
                if (child is WebView) {
                    child.destroy()
                }
            }
            frame.removeAllViews()
        },
    )
}
