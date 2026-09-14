package com.ericmbpeck.ai_sum.usage

import android.annotation.SuppressLint
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume

object CodexWebViewReader : UsageReader {
    const val SIGN_IN_URL: String = "https://chatgpt.com/auth/login"
    const val USAGE_URL: String = "https://chatgpt.com/#settings/Usage"

    override val signInUrl: String = SIGN_IN_URL
    override val readUrl: String = USAGE_URL
    override val hostLabel: String = "chatgpt.com"

    private const val READ_TIMEOUT_MS = 20_000L
    private const val SPA_SETTLE_MS = 1_200L
    private const val USAGE_WAIT_ATTEMPTS = 15
    private const val USAGE_WAIT_MS = 500L
    private const val FAILED_RETRIES = 3
    private const val FAILED_RETRY_MS = 800L

    override suspend fun read(webView: WebView): UsageReadResult {
        val url = withContext(Dispatchers.Main) { webView.url.orEmpty() }
        when {
            CodexHosts.isChatGptHost(url) -> {
                if (!CodexHosts.hasUsageSurface(url)) {
                    ensureUsageOrigin(webView)
                }
            }
            url.isBlank() || url == "about:blank" -> ensureUsageOrigin(webView)
            else -> return UsageReadResult.NeedsSignIn
        }
        ensureUsageHash(webView)
        delay(SPA_SETTLE_MS)
        waitForUsageSheet(webView)
        var last: UsageReadResult = UsageReadResult.Failed("Could not read usage")
        repeat(FAILED_RETRIES + 1) { attempt ->
            if (attempt > 0) delay(FAILED_RETRY_MS)
            val raw = withTimeoutOrNull(READ_TIMEOUT_MS) {
                withContext(Dispatchers.Main) {
                    evaluateFetch(webView)
                }
            } ?: return UsageReadResult.Failed("Timed out reading usage")
            last = CodexUsageMapper.map(raw, Instant.now(), ZoneId.systemDefault())
            if (last !is UsageReadResult.Failed) return last
        }
        return last
    }

    private suspend fun waitForUsageSheet(webView: WebView) {
        repeat(USAGE_WAIT_ATTEMPTS) {
            if (pageShowsUsage(webView)) return
            delay(USAGE_WAIT_MS)
        }
    }

    private suspend fun pageShowsUsage(webView: WebView): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                webView.evaluateJavascript(CODEX_USAGE_VISIBLE_SCRIPT) { value ->
                    if (cont.isActive) {
                        cont.resume(value == "true" || value == "\"true\"")
                    }
                }
            }
        }

    private suspend fun ensureUsageHash(webView: WebView) {
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(CODEX_ENSURE_HASH_SCRIPT, null)
        }
    }

    private suspend fun ensureUsageOrigin(webView: WebView) {
        val already = withContext(Dispatchers.Main) {
            val current = webView.url
            CodexHosts.isChatGptHost(current) && CodexHosts.hasUsageSurface(current)
        }
        if (already) return
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val client = webView.webViewClient as? ClaudeWebViewClient
                    if (client == null) {
                        webView.loadUrl(USAGE_URL)
                        cont.resume(Unit)
                        return@suspendCancellableCoroutine
                    }
                    client.pageFinishedWaiter = { finishedUrl ->
                        if (CodexHosts.isChatGptHost(finishedUrl) && cont.isActive) {
                            client.pageFinishedWaiter = null
                            cont.resume(Unit)
                        }
                    }
                    webView.loadUrl(USAGE_URL)
                }
            }
        }
    }

    @SuppressLint("JavascriptInterface")
    private suspend fun evaluateFetch(webView: WebView): String =
        suspendCancellableCoroutine { cont ->
            val bridge = (webView.tag as? ClaudeJsBridge) ?: ClaudeJsBridge().also {
                webView.addJavascriptInterface(it, CLAUDE_BRIDGE_NAME)
                webView.addJavascriptInterface(it, USAGE_BRIDGE_NAME)
                webView.tag = it
            }
            bridge.onPayload = { json ->
                if (cont.isActive) {
                    cont.resume(json)
                }
            }
            cont.invokeOnCancellation { bridge.onPayload = null }
            webView.evaluateJavascript(CODEX_FETCH_SCRIPT, null)
        }
}

internal const val CODEX_ENSURE_HASH_SCRIPT: String =
    "(function(){var h=(location.hash||'').toLowerCase();" +
        "var p=(location.pathname||'').toLowerCase();" +
        "if(h.indexOf('settings/usage')===-1&&p.indexOf('/codex/settings/usage')===-1){" +
        "location.hash='settings/Usage';}})()"

internal const val CODEX_USAGE_VISIBLE_SCRIPT: String =
    "(function(){var t=(document.body&&document.body.innerText)||'';" +
        "var h=(location.hash||'').toLowerCase();" +
        "return h.indexOf('settings/usage')!==-1||" +
        "/5-hour|5 hour|weekly (limit|usage)|%\\s*used|codex usage/i.test(t);})()"

internal const val CODEX_FETCH_SCRIPT: String = """
(function() {
  if (typeof AISUM_USAGE_READ === 'undefined') { return; }
  var done = false;
  function send(obj) {
    if (done) return;
    done = true;
    try { AISUM_USAGE_READ.onResult(JSON.stringify(obj)); } catch (e) {}
  }
  var host = (location && location.hostname) ? location.hostname : '';
  if (host !== 'chatgpt.com' && host !== 'www.chatgpt.com' && host !== 'chat.openai.com') {
    send({ kind: 'wrong_origin', host: host });
    return;
  }
  function num(v) {
    if (v == null || v === '') return null;
    if (typeof v === 'number' && isFinite(v)) return v;
    var n = parseFloat(String(v).replace(/[%${'$'}\s,]/g, ''));
    return isFinite(n) ? n : null;
  }
  function planFromSession(session) {
    if (!session || typeof session !== 'object') return '';
    var account = session.account || (session.user && session.user.account) || {};
    return session.plan_type || session.planType || session.plan ||
      account.planType || account.plan_type || account.plan ||
      (session.user && (session.user.planType || session.user.plan)) || '';
  }
  function accessToken(session) {
    if (!session || typeof session !== 'object') return '';
    return session.accessToken || session.access_token ||
      (session.user && (session.user.accessToken || session.user.access_token)) || '';
  }
  function windowName(win) {
    if (!win) return 'Current session';
    var secs = num(win.limit_window_seconds);
    if (secs != null && secs >= 6 * 24 * 3600) return 'Weekly limit';
    var n = String(win.name || win.label || '').toLowerCase();
    if (/week|seven/.test(n)) return 'Weekly limit';
    return 'Current session';
  }
  function windowItem(win) {
    if (!win || typeof win !== 'object') return null;
    var p = num(win.used_percent);
    if (p == null) p = num(win.percent);
    if (p == null) p = num(win.utilization);
    if (p == null) return null;
    if (p < 0 || p > 100) return null;
    var resets = win.reset_at || win.resets_at || win.resetsAt || null;
    return {
      name: windowName(win),
      percent: p,
      resets_at: resets == null ? null : String(resets),
      reset_at: resets,
      limit_window_seconds: win.limit_window_seconds,
      reset_after_seconds: win.reset_after_seconds
    };
  }
  function fromWham(json, plan) {
    if (!json || typeof json !== 'object') return null;
    var p = plan || json.plan_type || json.planType || json.plan || '';
    var rl = json.rate_limit || json.rateLimit || {};
    var windows = [];
    var a = windowItem(rl.primary_window || rl.primaryWindow);
    var b = windowItem(rl.secondary_window || rl.secondaryWindow);
    if (a) windows.push(a);
    if (b) windows.push(b);
    if (!windows.length) return null;
    return { kind: 'ok', plan: String(p), plan_type: String(p), windows: windows, rate_limit: rl };
  }
  function labeledPercent(text, label) {
    var re = new RegExp(label + '[\\s\\S]{0,120}?(\\d+(?:\\.\\d+)?)\\s*%', 'i');
    var m = text.match(re);
    return m ? num(m[1]) : null;
  }
  function fromDom(plan) {
    var text = (document.body && document.body.innerText) ? document.body.innerText : '';
    var slice = text;
    var idx = text.search(/\bCodex\b/i);
    if (idx >= 0) slice = text.slice(idx, idx + 1600);
    var sessionP = labeledPercent(slice, '5-hour') || labeledPercent(slice, '5 hour') ||
      labeledPercent(slice, 'Current session') || labeledPercent(slice, 'session');
    var weeklyP = labeledPercent(slice, 'Weekly') || labeledPercent(text, 'Weekly');
    if (sessionP == null && weeklyP == null) {
      var used = slice.match(/(\d+(?:\.\d+)?)\s*%\s*used/i) || text.match(/(\d+(?:\.\d+)?)\s*%\s*used/i);
      if (used) sessionP = num(used[1]);
    }
    if (sessionP == null && weeklyP == null) return null;
    var resets = null;
    var iso = text.match(/\d{4}-\d{2}-\d{2}T[^\s]+/);
    if (iso) resets = iso[0];
    if (!resets) {
      var rm = text.match(/Resets\s+([A-Za-z]+ \d{1,2}, \d{4}(?: at \d{1,2}:\d{2}\s*[AP]M)?)/i);
      if (rm) resets = rm[1];
    }
    var windows = [];
    if (sessionP != null) windows.push({ name: 'Current session', percent: sessionP, resets_at: resets });
    if (weeklyP != null) windows.push({ name: 'Weekly limit', percent: weeklyP, resets_at: resets });
    var pagePlan = '';
    if (/\bchatgpt\s+pro\b|\bpro\s*20|\bpro\s*5/i.test(text)) pagePlan = 'Pro';
    else if (/\bchatgpt\s+plus\b|\bplus\b/i.test(text) && !/plus\s*to/i.test(text)) pagePlan = 'Plus';
    else if (/\bbusiness\b/i.test(text)) pagePlan = 'Business';
    return { kind: 'ok', plan: String(plan || pagePlan), plan_type: String(plan || pagePlan), windows: windows };
  }
  function pageLooksSignedOut() {
    var t = (document.body && document.body.innerText) ? document.body.innerText : '';
    if (/5-hour|weekly (limit|usage)|% used|codex usage/i.test(t)) return false;
    return /log in|sign up|create an account|continue with google/i.test(t) &&
      /auth\/login|\/login/i.test(location.pathname || '');
  }
  function looksUsage(url) {
    return /wham\/usage|conversation_limit|settings\/usage|rate.?limit|codex\/settings/i.test(url);
  }
  (async function() {
    try {
      if (pageLooksSignedOut()) {
        send({ kind: 'needs_sign_in' });
        return;
      }
      var session = null;
      try {
        var sres = await fetch('/api/auth/session', {
          credentials: 'include',
          headers: { 'Accept': 'application/json' }
        });
        if (sres.status === 401 || sres.status === 403) {
          send({ kind: 'needs_sign_in' });
          return;
        }
        if (sres.ok) session = await sres.json();
      } catch (e) {}
      var plan = planFromSession(session);
      var token = accessToken(session);
      if (token) {
        try {
          var ures = await fetch('/backend-api/wham/usage', {
            credentials: 'include',
            headers: {
              'Authorization': 'Bearer ' + token,
              'Accept': 'application/json'
            }
          });
          if (ures.status === 401 || ures.status === 403) {
            send({ kind: 'needs_sign_in' });
            return;
          }
          if (ures.ok) {
            var mapped = fromWham(await ures.json(), plan);
            if (mapped) { send(mapped); return; }
          }
        } catch (e) {}
      }
      var entries = [];
      try { entries = performance.getEntriesByType('resource') || []; } catch (e) {}
      var seen = {};
      for (var i = 0; i < entries.length; i++) {
        var url = entries[i].name || '';
        if (!url || seen[url] || !looksUsage(url)) continue;
        var originOk = false;
        try { originOk = new URL(url, location.href).origin === location.origin; } catch (e) {}
        if (!originOk) continue;
        seen[url] = true;
        try {
          var res = await fetch(url, { credentials: 'include', headers: { 'Accept': 'application/json' } });
          if (!res.ok) continue;
          var mappedPerf = fromWham(await res.json(), plan);
          if (mappedPerf) { send(mappedPerf); return; }
        } catch (e) {}
      }
      var mappedDom = fromDom(plan);
      if (mappedDom) { send(mappedDom); return; }
      send({ kind: 'error', message: 'Could not read usage' });
    } catch (e) {
      send({ kind: 'error', message: 'Could not read usage' });
    }
  })();
})();
"""
