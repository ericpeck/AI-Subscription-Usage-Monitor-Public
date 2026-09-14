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

object CursorWebViewReader : UsageReader {
    const val SIGN_IN_URL: String = "https://cursor.com/dashboard"
    const val SPENDING_URL: String = "https://cursor.com/dashboard/spending"

    override val signInUrl: String = SIGN_IN_URL
    override val readUrl: String = SPENDING_URL
    override val hostLabel: String = "cursor.com"

    private const val READ_TIMEOUT_MS = 20_000L
    private const val SPA_SETTLE_MS = 1_200L
    private const val USAGE_WAIT_ATTEMPTS = 15
    private const val USAGE_WAIT_MS = 500L
    private const val FAILED_RETRIES = 3
    private const val FAILED_RETRY_MS = 800L

    override suspend fun read(webView: WebView): UsageReadResult {
        val url = withContext(Dispatchers.Main) { webView.url.orEmpty() }
        when {
            CursorHosts.isCursorHost(url) -> {
                if (!CursorHosts.hasSpendingPath(url)) {
                    ensureSpendingOrigin(webView)
                }
            }
            url.isBlank() || url == "about:blank" -> ensureSpendingOrigin(webView)
            else -> return UsageReadResult.NeedsSignIn
        }
        delay(SPA_SETTLE_MS)
        waitForSpendingSheet(webView)
        var last: UsageReadResult = UsageReadResult.Failed("Could not read usage")
        repeat(FAILED_RETRIES + 1) { attempt ->
            if (attempt > 0) delay(FAILED_RETRY_MS)
            val raw = withTimeoutOrNull(READ_TIMEOUT_MS) {
                withContext(Dispatchers.Main) {
                    evaluateFetch(webView)
                }
            } ?: return UsageReadResult.Failed("Timed out reading usage")
            last = CursorUsageMapper.map(raw, Instant.now(), ZoneId.systemDefault())
            if (last !is UsageReadResult.Failed) return last
        }
        return last
    }

    private suspend fun waitForSpendingSheet(webView: WebView) {
        repeat(USAGE_WAIT_ATTEMPTS) {
            if (pageShowsSpending(webView)) return
            delay(USAGE_WAIT_MS)
        }
    }

    private suspend fun pageShowsSpending(webView: WebView): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                webView.evaluateJavascript(CURSOR_SPENDING_VISIBLE_SCRIPT) { value ->
                    if (cont.isActive) {
                        cont.resume(value == "true" || value == "\"true\"")
                    }
                }
            }
        }

    private suspend fun ensureSpendingOrigin(webView: WebView) {
        val already = withContext(Dispatchers.Main) {
            val current = webView.url
            CursorHosts.isCursorHost(current) && CursorHosts.hasSpendingPath(current)
        }
        if (already) return
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val client = webView.webViewClient as? ClaudeWebViewClient
                    if (client == null) {
                        webView.loadUrl(SPENDING_URL)
                        cont.resume(Unit)
                        return@suspendCancellableCoroutine
                    }
                    client.pageFinishedWaiter = { finishedUrl ->
                        if (
                            CursorHosts.isCursorHost(finishedUrl) &&
                            CursorHosts.hasSpendingPath(finishedUrl) &&
                            cont.isActive
                        ) {
                            client.pageFinishedWaiter = null
                            cont.resume(Unit)
                        }
                    }
                    webView.loadUrl(SPENDING_URL)
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
            webView.evaluateJavascript(CURSOR_FETCH_SCRIPT, null)
        }
}

internal const val CURSOR_SPENDING_VISIBLE_SCRIPT: String =
    "(function(){var t=(document.body&&document.body.innerText)||'';" +
        "return /spending|included|cursor models|other models|%\\s*used|\\\$\\s*\\d/i.test(t);})()"

internal const val CURSOR_FETCH_SCRIPT: String = """
(function() {
  if (typeof AISUM_USAGE_READ === 'undefined') { return; }
  var done = false;
  function send(obj) {
    if (done) return;
    done = true;
    try { AISUM_USAGE_READ.onResult(JSON.stringify(obj)); } catch (e) {}
  }
  var host = (location && location.hostname) ? location.hostname : '';
  if (host !== 'cursor.com' && host !== 'www.cursor.com') {
    send({ kind: 'wrong_origin', host: host });
    return;
  }
  function num(v) {
    if (v == null || v === '') return null;
    if (typeof v === 'number' && isFinite(v)) return v;
    var n = parseFloat(String(v).replace(/[%${'$'}\s,]/g, ''));
    return isFinite(n) ? n : null;
  }
  var PERCENT_KEYS = ['percent','used_percent','usedPercent','percent_used','percentUsed','usage_percent','usagePercent','credit_usage_percent','utilization'];
  function pickPercent(obj) {
    if (!obj || typeof obj !== 'object') return null;
    for (var i = 0; i < PERCENT_KEYS.length; i++) {
      var n = num(obj[PERCENT_KEYS[i]]);
      if (n == null) continue;
      if (PERCENT_KEYS[i] === 'utilization' && n <= 1) n = n * 100;
      if (n < 0 || n > 100) continue;
      return n;
    }
    var used = num(obj.used || obj.spend || obj.spent || obj.included);
    var limit = num(obj.limit || obj.allowance || obj.max || obj.included_limit);
    if (used != null && limit != null && limit > 0) {
      var p = (used / limit) * 100;
      if (p >= 0 && p <= 100) return p;
    }
    return null;
  }
  function looksAuth(url) {
    return /login|signin|sign-in|oauth|authorize|logout/i.test(url);
  }
  function looksUsage(url) {
    return /spending|usage|credit|billing|quota|invoice|allowance|entitlement/i.test(url);
  }
  function windowItem(name, percent, resets) {
    if (!name || percent == null) return null;
    return { name: String(name), percent: percent, resets_at: resets ? String(resets) : null };
  }
  function windowsFrom(obj) {
    var out = [];
    if (!obj) return out;
    var list = obj.windows || obj.pools || obj.categories || obj.breakdown;
    if (Array.isArray(list)) {
      for (var i = 0; i < list.length; i++) {
        var item = list[i];
        if (!item) continue;
        var name = item.name || item.product || item.label || item.id || '';
        if (/grok\s*bot/i.test(String(name))) continue;
        var p = pickPercent(item);
        if (p == null) p = num(item.value);
        var w = windowItem(name, p, item.resets_at || item.resetsAt || item.reset_at);
        if (w) out.push(w);
      }
    }
    var included = pickPercent(obj.included || obj.cursor_models || obj.cursorModels);
    if (included != null) out.push(windowItem('Cursor Models', included, obj.resets_at || obj.resetsAt));
    var other = pickPercent(obj.api || obj.other_models || obj.otherModels || obj.third_party);
    if (other != null) out.push(windowItem('Other Models', other, obj.resets_at || obj.resetsAt));
    return out;
  }
  function fromJson(json) {
    if (!json || typeof json !== 'object') return null;
    var windows = windowsFrom(json).concat(windowsFrom(json.usage)).concat(windowsFrom(json.data));
    var percent = pickPercent(json);
    if (percent == null && json.usage) percent = pickPercent(json.usage);
    if (percent == null && json.data) percent = pickPercent(json.data);
    var resets = json.resets_at || json.resetsAt || json.reset_at || json.resetAt ||
      (json.usage && (json.usage.resets_at || json.usage.resetsAt)) ||
      (json.data && json.data.resets_at);
    var plan = json.plan || json.plan_name || json.tier || '';
    if (json.subscription) {
      var s = json.subscription;
      if (typeof s === 'string') plan = plan || s;
      else plan = plan || s.plan || s.plan_name || s.tier || s.name || '';
    }
    if (plan && typeof plan === 'object') plan = plan.name || plan.slug || plan.type || '';
    if (!windows.length && percent == null) return null;
    var pagePlan = planFromText((document.body && document.body.innerText) || '');
    if (pagePlan) plan = pagePlan;
    var grokBot = grokBotFromText((document.body && document.body.innerText) || '');
    if (!grokBot && json.grok_bot && typeof json.grok_bot === 'object') grokBot = json.grok_bot;
    return {
      kind: 'ok',
      percent: percent,
      resets_at: resets ? String(resets) : null,
      plan: String(plan || ''),
      windows: windows,
      grok_bot: grokBot
    };
  }
  function grokBotFromText(text) {
    var idx = text.search(/\bGrok Bot\b/i);
    if (idx < 0) return null;
    var slice = text.slice(idx, idx + 600);
    var p = labeledPercent(slice, 'Weekly usage');
    if (p == null) {
      var used = slice.match(/(\d+(?:\.\d+)?)\s*%\s*used/i);
      if (used) p = num(used[1]);
    }
    if (p == null) return null;
    var rm = slice.match(/Resets\s+([A-Za-z]{3}\s+\d{1,2})/i);
    return { percent: p, resets_at: rm ? rm[1] : null };
  }
  function planFromText(text) {
    var current = text.match(/CURRENT PLAN[\s\S]{0,60}?(Pro\+|Ultra|Pro|Business|Team|Enterprise)/i);
    if (current) return current[1];
    var stripped = text.replace(/UPGRADE AVAILABLE[\s\S]{0,240}/ig, ' ')
      .replace(/upgrade\s+to\s+ultra/ig, ' ');
    if (/pro\+/i.test(stripped)) return 'Pro+';
    if (/\bultra\b/i.test(stripped)) return 'Ultra';
    if (/\bpro\b/i.test(stripped)) return 'Pro';
    if (/\bbusiness\b/i.test(stripped)) return 'Business';
    return '';
  }
  function pageLooksSignedOut() {
    var t = (document.body && document.body.innerText) ? document.body.innerText : '';
    if (/cursor models|other models|included usage|spending/i.test(t) && /\d/.test(t)) return false;
    if (/\d+\s*%/.test(t)) return false;
    return /sign in with (google|github|apple)/i.test(t) || /continue with google/i.test(t);
  }
  function labeledPercent(text, label) {
    var re = new RegExp(label + '[\\s\\S]{0,80}?(\\d+(?:\\.\\d+)?)\\s*%', 'i');
    var m = text.match(re);
    return m ? num(m[1]) : null;
  }
  function labeledDollars(text, label) {
    var re = new RegExp(label + '[\\s\\S]{0,80}?\\\$([\\d,.]+)\\s*(?:of|/)\\s*\\\$([\\d,.]+)', 'i');
    var m = text.match(re);
    if (!m) return null;
    var used = num(m[1]);
    var limit = num(m[2]);
    if (used == null || limit == null || limit <= 0) return null;
    var p = (used / limit) * 100;
    return (p >= 0 && p <= 100) ? p : null;
  }
  function fromDom() {
    var text = (document.body && document.body.innerText) ? document.body.innerText : '';
    var resets = null;
    var iso = text.match(/\d{4}-\d{2}-\d{2}T[^\s]+/);
    if (iso) resets = iso[0];
    if (!resets) {
      var rm = text.match(/Resets\s+([A-Za-z]+ \d{1,2}, \d{4}(?: at \d{1,2}:\d{2}\s*[AP]M)?)/i);
      if (rm) resets = rm[1];
    }
    if (!resets) {
      var monthly = text.match(/Usage limits reset on\s+([A-Za-z]{3,9}\s+\d{1,2}(?:,\s+\d{4})?)/i);
      if (monthly) resets = monthly[1];
    }
    var windows = [];
    var cursorP = labeledPercent(text, 'Cursor Models') || labeledPercent(text, 'Included') ||
      labeledDollars(text, 'Cursor Models') || labeledDollars(text, 'Included');
    var otherP = labeledPercent(text, 'Other Models') || labeledPercent(text, 'API') ||
      labeledDollars(text, 'Other Models') || labeledDollars(text, 'API');
    if (cursorP != null) windows.push({ name: 'Cursor Models', percent: cursorP, resets_at: resets });
    if (otherP != null) windows.push({ name: 'Other Models', percent: otherP, resets_at: resets });
    var percent = null;
    var used = text.match(/(\d+(?:\.\d+)?)\s*%\s*used/i);
    if (used) percent = num(used[1]);
    if (percent == null) {
      var dollars = text.match(/\$([\d,.]+)\s*(?:of|\/)\s*\$([\d,.]+)/i);
      if (dollars) {
        var u = num(dollars[1]);
        var lim = num(dollars[2]);
        if (u != null && lim != null && lim > 0) percent = (u / lim) * 100;
      }
    }
    if (percent == null && !windows.length) {
      var bars = document.querySelectorAll('[role="progressbar"], progress');
      for (var i = 0; i < bars.length; i++) {
        var el = bars[i];
        var n = num(el.getAttribute('aria-valuenow') || el.getAttribute('value'));
        if (n != null && n >= 0 && n <= 100) { percent = n; break; }
      }
    }
    if (percent == null && !windows.length) {
      var m = text.match(/(\d+(?:\.\d+)?)\s*%/);
      if (m) percent = num(m[1]);
    }
    var plan = planFromText(text);
    var grokBot = grokBotFromText(text);
    if (!windows.length && percent == null) return null;
    return { kind: 'ok', percent: percent, resets_at: resets, plan: plan, windows: windows, grok_bot: grokBot };
  }
  (async function() {
    try {
      if (pageLooksSignedOut()) {
        send({ kind: 'needs_sign_in' });
        return;
      }
      var sawAuthFail = false;
      var entries = [];
      try { entries = performance.getEntriesByType('resource') || []; } catch (e) {}
      var seen = {};
      for (var i = 0; i < entries.length; i++) {
        var url = entries[i].name || '';
        if (!url || seen[url]) continue;
        if (url.indexOf('api2.cursor.sh') !== -1) continue;
        if (url.indexOf('aiserver') !== -1) continue;
        if (looksAuth(url) || !looksUsage(url)) continue;
        var originOk = false;
        try { originOk = new URL(url, location.href).origin === location.origin; } catch (e) {}
        if (!originOk) continue;
        seen[url] = true;
        try {
          var res = await fetch(url, { credentials: 'include', headers: { 'Accept': 'application/json' } });
          if (res.status === 401 || res.status === 403) {
            sawAuthFail = true;
            continue;
          }
          if (!res.ok) continue;
          var mapped = fromJson(await res.json());
          if (mapped) { send(mapped); return; }
        } catch (e) {}
      }
      var mappedDom = fromDom();
      if (mappedDom) { send(mappedDom); return; }
      if (sawAuthFail) {
        send({ kind: 'needs_sign_in' });
        return;
      }
      send({ kind: 'error', message: 'Could not read usage' });
    } catch (e) {
      send({ kind: 'error', message: 'Could not read usage' });
    }
  })();
})();
"""
