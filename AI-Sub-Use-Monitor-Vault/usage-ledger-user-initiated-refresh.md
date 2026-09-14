# Usage Ledger — User-initiated refresh (Claude + Gemini)

**Date:** 2026-09-04  
**Status:** Architecture sketch. **Do not implement Tier A as the product UI.**  
**Context:** Background cookie polling of Claude’s undocumented `/usage` API is broken (HTTP 403; Claude-Counter-Android deprecated that path). User rejected pure manual entry. Preferred approach in *this* sketch: **load the real usage page whenever the provider screen is opened or the refresh control is tapped.**

---

## Relation to shipping AI-SUM (2026-09-04 evening)

The board’s product is native fill + hairline on `4a–4i`, not the provider’s own settings page. Eric’s Claude-first call is: sign in inside a WebView, then in-page `fetch()` of `/usage` onto those native bars. That is this sketch’s **Tier B**, session-bound and user-initiated — not Tier A.

| Sketch | Shipping code |
|---|---|
| No silent WorkManager poll, no OkHttp cookie lift | Hard rule. Keep. |
| Claude: in-app WebView, cookies stay there | Hard rule. Keep. Custom Tabs would isolate the session from `fetch()`. |
| Refresh on ↻ (and on screen appear) | ⟳ is wired for live Claude. **Read-on-open on 4a is claimed in the data-layer note and not actually called yet.** |
| Tier A: embed/open the official usage page as the UI | **Out.** Would replace the usage bar. Claude’s usage page can be a fallback if parse fails; it is not 4a. |
| Gemini: Custom Tabs, not WebView | Correct. See [[Gemini]] + [[Gemini-usage-data-layer]]. Stay fake until a live deep link. |
| "Not UA spoofing WebView as Chrome to force Google login" | **Superseded.** `ClaudeHosts.chromeLikeUserAgent()` strips `; wv` **and** `Version/4.0 ` — both markers Google's embedded-browser check keys on, and that check does not vary by relying party. Standing rule now lives in [[AGENTS]]: normalize the UA so Google-as-IdP completes for third-party relying parties (claude.ai, later chatgpt.com); never to sign in to Google first-party surfaces (Gemini). |

Durable rules: [[Claude-usage-data-layer]], [[Gemini-usage-data-layer]], [[AGENTS]].

---

## Goal

For each AI subscription screen, show current usage (allowance % used and window % elapsed) by **refreshing the official usage UI on demand**, not by silent background harvest.

Two tiers (ship in order):

| Tier | Behavior | Risk |
|------|----------|------|
| **A — Page as UI** | Embed or open the provider’s own usage page. User sees bars where they already live. | Lowest. No scrape/parse. |
| **B — Native cards (optional later)** | Same open/refresh trigger; while the WebView/session is alive, read structured usage (intercept `/usage`, SSE, or DOM) into Usage Ledger’s two numbers. | Higher ToS / fingerprint risk; still **user-initiated**, not a background service. |

v1 recommendation: **ship Tier A**; design APIs so Tier B can plug in without changing the open/refresh contract.

---

## Shared app contract

### Triggers (all providers)

1. **Screen appear** — first `onResume` / composition enter after navigation to that provider’s screen → start a refresh.
2. **Explicit refresh** — toolbar/FAB refresh button or pull-to-refresh → same refresh path.
3. **No silent poll** — no `WorkManager` / foreground service hitting usage APIs with stored cookies while the user is elsewhere.

Optional later (out of scope for this doc): OS widget that only shows **last cached** snapshot until the user opens the app.

### Shared UI chrome (native)

```
┌─────────────────────────────────────┐
│  ← Claude                    ↻     │  ← refresh button
│─────────────────────────────────────│
│  [optional Tier B summary cards]    │  ← empty / last-known until parse exists
│─────────────────────────────────────│
│                                     │
│         Provider shell              │  ← WebView (Claude) or CCT handoff (Gemini)
│         (usage page)                │
│                                     │
└─────────────────────────────────────┘
```

### State machine (per provider)

```
Idle ──open / ↻──► Loading ──success──► Ready (page shown; optional cache stamp)
                 │
                 └──auth needed──► NeedsSignIn ──user signs in──► Loading
                 │
                 └──error──────────► Error (retry = same refresh path)
```

- Persist **last successful refresh time** (and Tier B numbers if any) in local Room/DataStore.
- On open: show **stale cache** immediately if present, then kick refresh (stale-while-revalidate).

---

## Claude — in-app WebView

### Why WebView works here

- Claude-Counter’s failure mode was **background** `HttpURLConnection`/`OkHttp` with cookies + custom UA (`ClaudeCounter/1.0 Android`) → **403**.
- User-visible WebView navigations to `claude.ai` with a normal WebView UA keep same-origin cookies and look like a real client session.
- Prior art: Claude-Counter still used WebView successfully for **login**; only the detached poll died.

### Target URL

Prefer the usage surface the user already knows:

- Primary: Claude **Settings → Usage** (confirm exact deep link at implement time; often reachable from account/settings on `claude.ai`).
- Fallback: `https://claude.ai/` then guide user once if deep link drifts.

Treat exact path as **REQUIRES LIVE INSPECTION** if undocumented; store URL in a single config constant.

### Lifecycle

| Event | Action |
|-------|--------|
| First enter / cold start | Create WebView; restore cookies if any; `loadUrl(usageUrl)`. |
| `onResume` | If `shouldReloadOnResume` (default **true** when returning from another app screen, or always if last refresh older than N minutes — start with **always reload on resume** for honesty). |
| Refresh button | `webView.reload()` **or** `loadUrl(usageUrl)` (prefer full load of usage URL so SPA state resets). |
| `onPause` | Do **not** clear cookies; optionally pause timers/JS. |
| Sign-out (user action) | Clear WebView cookie store + local cache for Claude. |

Avoid: extracting cookies and replaying them from a **non-WebView** HTTP client in a service.

### Cookie / session persistence

- Use Android `CookieManager` tied to the Claude WebView (`setAcceptCookie(true)`, sync on Lollipop+ as needed).
- Persist across process death via the system cookie store (default WebView behavior) unless you force private browsing.
- Do **not** copy cookies into SharedPreferences for “API reuse” in v1.

### Login flow

1. If usage URL redirects to login → WebView stays; user signs in inside WebView.
2. After redirect back to usage → Ready.
3. No separate “paste cookie” or desktop OAuth transfer required for Tier A.

### Tier B (optional later, Claude only)

While WebView is foreground and session is warm:

- Prefer **in-page** observation: WebViewClient / shouldInterceptRequest for same-origin `…/organizations/{id}/usage`, or JS interface reading visible bars (fragile).
- Or listen for SSE `message_limit` only while the WebView is open (extension pattern, port carefully).
- Never schedule those requests from a background service with exported cookies.

### ToS / product notes

- Consumer Terms restrict scrape/harvest and automated non-human access; **user driving a WebView of the official UI** is the conservative interpretation for Tier A.
- Tier B still sits closer to undocumented client APIs — document risk; do not claim official support.

---

## Gemini — Chrome Custom Tabs (not embedded WebView)

### Why not WebView

Google Account sign-in in embedded WebViews is blocked (`disallowed_useragent` / disallowed user agent policy). Claude-style “login in WebView then reuse cookies” **does not port**.

### Shell

Use **Chrome Custom Tabs** (AndroidX Browser):

1. On screen open / refresh → `CustomTabsIntent.launchUrl(activity, usageUri)`.
2. Native Usage Ledger screen can show:
   - Last-known Tier B stats (if any), and/or
   - A large “Open Gemini usage” / “Refresh in browser” card that mirrors the refresh affordance.
3. When the user returns (`onResume`), update “last opened” timestamp; do **not** assume you scraped anything (Tier A = CCT is the truth UI).

### Target URL

- Gemini Apps **Usage Limits** (support doc: [Gemini Apps limits](https://support.google.com/gemini/answer/16275805)).
- Exact in-product deep link **REQUIRES LIVE INSPECTION** (often settings within `gemini.google.com` / related Google AI surfaces). Config constant + fallback to account settings entry.

### Lifecycle

| Event | Action |
|-------|--------|
| Screen appear | Optionally auto-launch CCT once per appear **or** show landing card + require tap (prefer **tap or refresh** to avoid surprising tab spam; auto-launch only if product wants parity with Claude WebView). |
| Refresh button | Launch/re-launch CCT to usage URL (Custom Tabs may reuse session if Chrome already signed in). |
| `onResume` after CCT | Mark refresh attempted; show tip “Numbers update in Gemini; pull Usage Ledger after you check.” until Tier B exists. |
| No CookieManager bridge | Do not attempt to read Chrome’s cookie jar from the app. |

**Product choice (decide at implement):**

- **A1 — Embedded feel:** Claude = WebView fills the screen; Gemini = landing + “Open usage” (asymmetric but honest).
- **A2 — Symmetric open:** Both “refresh” always open the provider surface (WebView vs CCT). Gemini screen is thinner natively.

Recommend **A1** for v1 clarity.

### Tier B for Gemini

Still unresolved: no proven consumer “remaining %” JSON akin to Claude’s `/usage`; `batchexecute` undocumented; PSID cookies rotate; ToS/automation risk. **Do not block v1 on Gemini Tier B.** Tier A CCT is enough to avoid “manual number entry.”

---

## Android sketch (interfaces)

```kotlin
sealed class ProviderShell {
  data class InAppWebView(val usageUrl: String) : ProviderShell()
  data class CustomTab(val usageUrl: String) : ProviderShell()
}

interface UsageRefresh {
  /** Called on screen appear (policy) and on explicit refresh. */
  fun refresh(reason: RefreshReason)
}

enum class RefreshReason { SCREEN_APPEAR, USER_BUTTON, PULL }
```

### Claude screen (pseudo)

```
onCreate:
  bind WebView + CookieManager
  webView.settings.javaScriptEnabled = true  // required for claude.ai SPA
  // sensible defaults: DOM storage, no third-party cookies looseness beyond need

onResume:
  if (policy.shouldRefreshOnResume(lastRefreshAt)) refresh(SCREEN_APPEAR)

onRefreshClick:
  refresh(USER_BUTTON)  // loadUrl(CLAUDE_USAGE_URL)

onSignOut:
  CookieManager.removeAllCookies…
  clear local Claude cache
```

### Gemini screen (pseudo)

```
onRefreshClick / optional onFirstAppear:
  CustomTabsIntent.Builder()
    .setShowTitle(true)
    .build()
    .launchUrl(this, Uri.parse(GEMINI_USAGE_URL))

onResume:
  // update lastOpenedAt; no cookie scrape
```

### Navigation

One destination per provider (`claude`, `gemini`, …). Shared `ProviderUsageScaffold(title, onRefresh, content)`.

---

## What this is *not*

- Not background notification polling (Claude-Counter’s dead path).
- Not UA modification to sign in to **Google first-party surfaces** (Gemini). The WebView UA *is* normalized — `; wv` and `Version/4.0 ` stripped — so Google-as-IdP completes for third-party relying parties: claude.ai today, chatgpt.com later. See [[AGENTS]] → Standing rules.
- Not asking the user to type percentages by hand.
- Not claiming an official Anthropic/Google consumer usage API.

---

## Implementation checklist

- [ ] Confirm Claude usage deep link on current `claude.ai` (live).
- [ ] Confirm Gemini Usage Limits deep link (live).
- [ ] Claude: WebView screen + refresh + cookie persistence + sign-out clear.
- [ ] Gemini: Custom Tabs launch + landing copy + refresh.
- [ ] Shared: last-refresh timestamp, error/empty states, no background usage poll.
- [ ] (Later) Claude Tier B parse only while WebView session alive.
- [ ] (Later) Revisit Gemini Tier B only if a documented or clearly user-session-bound path appears.

---

## Related research

- `/workspace/usage-ledger-anthropic-claude.md` / six-section brief — official APIs org-only; consumer `/usage` undocumented.
- `/workspace/usage-ledger-gemini-webview-findings.md` — no consumer Apps usage API; WebView sign-in blocked.
- Claude-Counter-Android — WebView login OK; background `/usage` → 403 (deprecated Apr 2026).

---

## Decision log

| Date | Decision |
|------|----------|
| 2026-09-04 | Prefer user-initiated page refresh over automation/manual entry. |
| 2026-09-04 | Claude → in-app WebView; Gemini → Chrome Custom Tabs. |
| 2026-09-04 | Tier A (show page) first; Tier B (native parse) optional and session-bound. |
