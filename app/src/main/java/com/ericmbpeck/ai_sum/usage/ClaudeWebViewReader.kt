package com.ericmbpeck.ai_sum.usage

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume

object ClaudeWebViewReader : UsageReader {
    override val signInUrl: String = "https://claude.ai/login"
    const val HOME_URL: String = "https://claude.ai/"
    override val readUrl: String = HOME_URL
    override val hostLabel: String = "claude.ai"

    private const val READ_TIMEOUT_MS = 20_000L

    override suspend fun read(webView: WebView): UsageReadResult {
        val url = withContext(Dispatchers.Main) { webView.url.orEmpty() }
        when {
            ClaudeHosts.isClaudeHost(url) -> Unit
            url.isBlank() || url == "about:blank" -> ensureClaudeOrigin(webView)
            else -> return UsageReadResult.NeedsSignIn
        }
        val raw = withTimeoutOrNull(READ_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                evaluateFetch(webView)
            }
        } ?: return UsageReadResult.Failed("Timed out reading usage")
        return ClaudeUsageMapper.map(raw, Instant.now(), ZoneId.systemDefault())
    }

    private suspend fun ensureClaudeOrigin(webView: WebView) {
        val already = withContext(Dispatchers.Main) { ClaudeHosts.isClaudeHost(webView.url) }
        if (already) return
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val client = webView.webViewClient as? ClaudeWebViewClient
                    if (client == null) {
                        webView.loadUrl(HOME_URL)
                        cont.resume(Unit)
                        return@suspendCancellableCoroutine
                    }
                    client.pageFinishedWaiter = { url ->
                        if (ClaudeHosts.isClaudeHost(url) && cont.isActive) {
                            client.pageFinishedWaiter = null
                            cont.resume(Unit)
                        }
                    }
                    webView.loadUrl(HOME_URL)
                }
            }
        }
    }

    @SuppressLint("JavascriptInterface")
    private suspend fun evaluateFetch(webView: WebView): String =
        suspendCancellableCoroutine { cont ->
            val bridge = (webView.tag as? ClaudeJsBridge) ?: ClaudeJsBridge().also {
                webView.addJavascriptInterface(it, CLAUDE_BRIDGE_NAME)
                webView.tag = it
            }
            bridge.onPayload = { json ->
                if (cont.isActive) {
                    cont.resume(json)
                }
            }
            cont.invokeOnCancellation { bridge.onPayload = null }
            webView.evaluateJavascript(FETCH_SCRIPT, null)
        }

    fun persistCookies() {
        CookieManager.getInstance().flush()
    }

    fun forgetSession() {
        WebViewSessionCookies.forgetAll()
    }
}

internal const val FETCH_SCRIPT: String = """
(function() {
  if (typeof AISUM_CLAUDE_READ === 'undefined') { return; }
  var done = false;
  function send(obj) {
    if (done) return;
    done = true;
    try { AISUM_CLAUDE_READ.onResult(JSON.stringify(obj)); } catch (e) {}
  }
  var host = (location && location.hostname) ? location.hostname : '';
  if (host !== 'claude.ai' && host !== 'www.claude.ai') {
    send({ kind: 'wrong_origin', host: host });
    return;
  }
  function firstOrg(json) {
    if (!json) return null;
    if (Array.isArray(json) && json[0]) return json[0];
    if (json.uuid || json.id) return json;
    var list = json.data || json.organizations || json.orgs;
    if (Array.isArray(list) && list[0]) return list[0];
    return null;
  }
  function planName(json) {
    if (!json) return '';
    var p = json.plan || json.plan_name || json.subscription_plan || json.tier ||
      json.rate_limit_tier || json.rateLimitTier || json.billing_plan || json.billingPlan || '';
    if (json.subscription) {
      var s = json.subscription;
      p = p || s.plan || s.plan_name || s.tier || s.rate_limit_tier || '';
    }
    if (json.settings) {
      var st = json.settings;
      p = p || st.plan || st.rate_limit_tier || st.rateLimitTier || '';
    }
    if (p && typeof p === 'object') p = p.name || p.slug || p.type || p.tier || '';
    return String(p || '');
  }
  function looksLikePlan(name) {
    return /pro|max|team|enterprise/i.test(String(name || ''));
  }
  (async function() {
    try {
      var orgRes = await fetch('/api/organizations', {
        credentials: 'include',
        headers: { 'Accept': 'application/json' }
      });
      if (orgRes.status === 401 || orgRes.status === 403) {
        send({ kind: 'needs_sign_in', status: orgRes.status });
        return;
      }
      if (!orgRes.ok) {
        send({ kind: 'error', status: orgRes.status, message: 'Could not read usage' });
        return;
      }
      var orgJson = await orgRes.json();
      var orgObj = firstOrg(orgJson);
      var id = orgObj && (orgObj.uuid || orgObj.id);
      if (!id) {
        send({ kind: 'needs_sign_in', status: orgRes.status });
        return;
      }
      var subscription = null;
      try {
        var subRes = await fetch('/api/organizations/' + id + '/subscription_details', {
          credentials: 'include',
          headers: { 'Accept': 'application/json' }
        });
        if (subRes.ok) subscription = await subRes.json();
      } catch (e) {}
      var usageRes = await fetch('/api/organizations/' + id + '/usage', {
        credentials: 'include',
        headers: { 'Accept': 'application/json' }
      });
      if (usageRes.status === 401 || usageRes.status === 403) {
        send({ kind: 'needs_sign_in', status: usageRes.status });
        return;
      }
      var usage = null;
      try { usage = await usageRes.json(); } catch (e) { usage = {}; }
      var name = planName(subscription);
      if (!looksLikePlan(name)) name = planName(orgObj);
      if (!looksLikePlan(name)) {
        try {
          var blob = JSON.stringify(orgObj || {}) + JSON.stringify(subscription || {});
          var m = blob.match(/default_claude(?:_ai)?_(max_?20x|max_?5x|max|pro)|claude_(max_?20x|max_?5x|max|pro)/i);
          if (m) name = m[0];
        } catch (e) {}
      }
      var five = usage && (usage.five_hour || usage.fiveHour ||
        (usage.usage && (usage.usage.five_hour || usage.usage.fiveHour)));
      var seven = usage && (usage.seven_day || usage.sevenDay ||
        (usage.usage && (usage.usage.seven_day || usage.usage.sevenDay)));
      var freeName = name && /(^|[^a-z])free([^a-z]|$)/i.test(name) && !/pro|max|team|enterprise/i.test(name);
      if (freeName || (usageRes.ok && !five && !seven)) {
        send({ kind: 'free', subscription: subscription, usage: usage, organization: orgObj });
        return;
      }
      if (!usageRes.ok) {
        send({ kind: 'error', status: usageRes.status, message: 'Could not read usage' });
        return;
      }
      send({ kind: 'ok', subscription: subscription, usage: usage, organization: orgObj, plan: name });
    } catch (e) {
      send({ kind: 'error', message: 'Could not read usage' });
    }
  })();
})();
"""
