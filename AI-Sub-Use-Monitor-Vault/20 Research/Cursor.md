---
platform: Cursor
board: 4f
researched: 2026-09-04
updated: 2026-09-09
status: researched
---

# Cursor usage

**Researched 2026-09-04; live path 2026-09-09.** Rule for the app: [[Cursor-usage-data-layer]].

## 1. The data source

### Sanctioned surfaces (human)

- **Start:** [https://cursor.com/dashboard](https://cursor.com/dashboard) — login (private testing used Google).
- **After login:** [https://cursor.com/agents](https://cursor.com/agents) — signed-in home, not usage.
- **Spending:** [https://cursor.com/dashboard/spending](https://cursor.com/dashboard/spending) — Settings at the bottom of the product. This is the page we parse onto native `4f`. Exhibit `Exibit1.png` (2026-09-09): CURRENT PLAN **Pro+ $60/mo**, Upgrade Ultra card, Cursor Models **27% used**, Other Models **12% used**, Grok Bot weekly **1% used** / Resets **Sep 13**. Grok Bot on this page is `4i`, not a third `4f` window.
- **IDE:** Settings usage summary (Usage Summary = Always).

Cursor’s June 2026 teams post describes included / Cursor models vs third-party API models — the same split as board `4f` when the spending page still shows two pools. If it is one meter, one fill.

### Public API for a standalone Android app

**None found.** Do not call `api2.cursor.sh` DashboardService RPCs. Do not lift cookies into OkHttp.

## 2. How authentication works

Login is on cursor.com (Google / GitHub / Apple). Google-as-IdP on **cursor.com** is allowed with the existing WebView UA strip (same as Claude). Do not use that strip to sign in to Gemini.

The IdP used in private testing is **Google**. Shipping 1d is the in-app WebView (same CookieManager as the later read). Custom Tabs would put the session in Chrome and cannot feed the native bar.

## 3. What we will and will not do

Shipping (see [[Cursor-usage-data-layer]]): in-app WebView, Google login, same-origin spending read onto native `4f`.

We will not:

- call `cli` / `api2.cursor.sh` / `GetCurrentPeriodUsage`
- embed Gemini-style first-party Google
- lift cookies into OkHttp
- auto-read unauthenticated `/agents` or `/dashboard` (post-SSO `/agents` after Google **is** auto-read; see [[Cursor-usage-data-layer]])
- start 1d on `/dashboard/spending`
- replace board `4f` with the spending page as the product UI
