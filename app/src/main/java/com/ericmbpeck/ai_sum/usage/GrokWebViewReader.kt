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

object GrokWebViewReader : UsageReader {
    const val HOME_URL: String = "https://grok.com/"
    const val USAGE_URL: String = "https://grok.com/?_s=usage"

    /**
     * xAI's own sign-in. grok.com home is a chat landing page with a cookie
     * banner — not the login form. [redirect] sends the session back to grok.com.
     */
    const val SIGN_IN_URL: String =
        "https://accounts.x.ai/sign-in?redirect=grok-com&return_to=%2F%3Fq%3D%26reasoningMode%3Dnone%26voice%3Dfalse"

    override val signInUrl: String = SIGN_IN_URL
    override val readUrl: String = USAGE_URL
    override val hostLabel: String = "grok.com"

    private const val READ_TIMEOUT_MS = 20_000L
    private const val SPA_SETTLE_MS = 1_200L
    private const val USAGE_WAIT_ATTEMPTS = 15
    private const val USAGE_WAIT_MS = 500L
    private const val FAILED_RETRIES = 3
    private const val FAILED_RETRY_MS = 800L

    override suspend fun read(webView: WebView): UsageReadResult {
        val url = withContext(Dispatchers.Main) { webView.url.orEmpty() }
        when {
            GrokHosts.isGrokHost(url) -> {
                if (!GrokHosts.hasUsageQuery(url)) {
                    ensureGrokOrigin(webView)
                }
            }
            url.isBlank() || url == "about:blank" -> ensureGrokOrigin(webView)
            else -> return UsageReadResult.NeedsSignIn
        }
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
            last = GrokUsageMapper.map(raw, Instant.now(), ZoneId.systemDefault())
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
                webView.evaluateJavascript(USAGE_VISIBLE_SCRIPT) { value ->
                    if (cont.isActive) {
                        cont.resume(value == "true" || value == "\"true\"")
                    }
                }
            }
        }

    private suspend fun ensureGrokOrigin(webView: WebView) {
        val already = withContext(Dispatchers.Main) {
            val current = webView.url
            GrokHosts.isGrokHost(current) && GrokHosts.hasUsageQuery(current)
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
                        if (
                            GrokHosts.isGrokHost(finishedUrl) &&
                            GrokHosts.hasUsageQuery(finishedUrl) &&
                            cont.isActive
                        ) {
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
            webView.evaluateJavascript(GROK_FETCH_SCRIPT, null)
        }
}

internal const val USAGE_VISIBLE_SCRIPT: String =
    "(function(){var t=(document.body&&document.body.innerText)||'';" +
        "return /%\\s*used|Weekly SuperGrok Limit/i.test(t);})()"

internal const val GROK_FETCH_SCRIPT: String = """
(function() {
  if (typeof AISUM_USAGE_READ === 'undefined') { return; }
  var done = false;
  function send(obj) {
    if (done) return;
    done = true;
    try { AISUM_USAGE_READ.onResult(JSON.stringify(obj)); } catch (e) {}
  }
  var host = (location && location.hostname) ? location.hostname : '';
  if (host !== 'grok.com' && host !== 'www.grok.com' && host !== 'grok.x.ai') {
    send({ kind: 'wrong_origin', host: host });
    return;
  }
  function num(v) {
    if (v == null || v === '') return null;
    if (typeof v === 'number' && isFinite(v)) return v;
    var n = parseFloat(String(v).replace(/[%\s,]/g, ''));
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
    return null;
  }
  function looksAuth(url) {
    return /login|signin|sign-in|oauth|authorize|logout/i.test(url);
  }
  function looksUsage(url) {
    return /usage|credit|billing|quota|rate.?limit|entitlement|allowance/i.test(url);
  }
  function productsFrom(obj) {
    var out = [];
    if (!obj) return out;
    var list = obj.products || obj.breakdown || obj.by_product || obj.product_usage;
    if (!Array.isArray(list)) return out;
    for (var i = 0; i < list.length; i++) {
      var item = list[i];
      if (!item) continue;
      var name = item.name || item.product || item.label || item.id || '';
      var p = pickPercent(item);
      if (p == null) p = num(item.value);
      if (name && p != null) out.push({ name: String(name), percent: p });
    }
    return out;
  }
  function fromJson(json) {
    if (!json || typeof json !== 'object') return null;
    var percent = pickPercent(json);
    if (percent == null && json.usage) percent = pickPercent(json.usage);
    if (percent == null && json.data) percent = pickPercent(json.data);
    var products = productsFrom(json).concat(productsFrom(json.usage)).concat(productsFrom(json.data));
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
    if (percent == null && products.length) {
      percent = 0;
      for (var i = 0; i < products.length; i++) percent += products[i].percent;
    }
    if (percent == null) return null;
    return {
      kind: 'ok',
      percent: percent,
      resets_at: resets ? String(resets) : null,
      plan: String(plan || ''),
      products: products
    };
  }
  function pageLooksSignedOut() {
    var t = (document.body && document.body.innerText) ? document.body.innerText : '';
    if (/\d+\s*%\s*used/i.test(t) || /weekly supergrok/i.test(t)) return false;
    if (/what should we explore/i.test(t)) return false;
    if (/\d+\s*%/.test(t)) return false;
    return /sign in with (x|google|apple)/i.test(t);
  }
  function fromDom() {
    var percent = null;
    var bars = document.querySelectorAll('[role="progressbar"], progress');
    for (var i = 0; i < bars.length; i++) {
      var el = bars[i];
      var n = num(el.getAttribute('aria-valuenow') || el.getAttribute('value'));
      if (n != null && n >= 0 && n <= 100) { percent = n; break; }
    }
    var text = (document.body && document.body.innerText) ? document.body.innerText : '';
    var used = text.match(/(\d+(?:\.\d+)?)\s*%\s*used/i);
    if (used) percent = num(used[1]);
    if (percent == null) {
      var m = text.match(/(\d+(?:\.\d+)?)\s*%/);
      if (m) percent = num(m[1]);
    }
    var products = [];
    var names = ['Voice','Chat','Coding','Build','Imagine','API'];
    for (var j = 0; j < names.length; j++) {
      var re = new RegExp(names[j] + '\\s*(\\d+(?:\\.\\d+)?)\\s*%', 'i');
      var pm = text.match(re);
      if (pm) products.push({ name: names[j], percent: num(pm[1]) });
    }
    var sum = 0;
    for (var k = 0; k < products.length; k++) sum += products[k].percent;
    if (products.length && (percent == null || sum > percent + 0.05)) percent = sum;
    var resets = null;
    var iso = text.match(/\d{4}-\d{2}-\d{2}T[^\s]+/);
    if (iso) resets = iso[0];
    if (!resets) {
      var rm = text.match(/Resets\s+([A-Za-z]+ \d{1,2}, \d{4} at \d{1,2}:\d{2}\s*[AP]M)/i);
      if (rm) resets = rm[1];
    }
    var plan = '';
    if (/supergrok/i.test(text)) plan = 'SuperGrok';
    else if (/\bheavy\b/i.test(text)) plan = 'Heavy';
    if (percent == null && !products.length) return null;
    if (percent == null) percent = sum;
    return { kind: 'ok', percent: percent, resets_at: resets, plan: plan, products: products };
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
        if (looksAuth(url) || !looksUsage(url)) continue;
        if (url.indexOf('cli-chat-proxy') !== -1) continue;
        if (url.indexOf('grpc') !== -1) continue;
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
