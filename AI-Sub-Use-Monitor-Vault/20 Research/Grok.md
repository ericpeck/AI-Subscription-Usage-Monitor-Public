---
platform: Grok
board: 4d
researched: 2026-09-04
updated: 2026-09-09
status: researched
---

# Grok usage

**Researched 2026-09-04.** Rule for the app: [[Grok-usage-data-layer]]. SuperGrok / grok.com only — not Grok Bot. See [[Grok Bot]].

Sources: `20 Research/Notes from Grok Bot/usage-ledger-cursor-supergrok-vs-claude.md`, plus a private live pass the same day.

## 1. The data source

### What the meter is

Since **June 2026** SuperGrok is described as one **shared weekly compute pool** across Chat / Imagine / Voice / Build (Usage also lists API in the breakdown). Numeric pool sizes per plan are **not published**.

Board `4d` paints that week as a **segmented** bar (Voice / Chat / Coding). That is design of record for the fake, not proof of three separate APIs. Live mapping: Build → Coding; extra products may appear as extra greys; total-only → one fill.

### Sanctioned surfaces (human)

- SuperGrok Settings → Usage on [grok.com](https://grok.com).
- Deep link: [https://grok.com/?_s=usage](https://grok.com/?_s=usage) (prefer grok.com over grok.x.ai).

**Private testing, 2026-09-04:** grok.com signed in with **X** as the identity provider. The usage page at `https://grok.com/?_s=usage` loaded a signed-in Usage sheet. That URL is confirmed live.

**Eric, 2026-09-09 recording `Screen_recording_20260909_124339.webm`:** after X login, Continue hydrates Usage late. Sheet copy is Weekly SuperGrok Limit **5% used**, Voice **3%**, Chat **2%**, `Resets September 10, 2026 at 3:01 PM`. Extra credits are not the weekly bar.

**Eric, 2026-09-09 recording `Screen_recording_20260909_131212.webm`:** after a clean X login the WebView reached grok.com chat; 1d stayed there until Continue. Auto-read must treat grok.com after an OAuth host as signed-in.

xAI **developer** RPS/TPM and Management billing are not the consumer SuperGrok weekly %. There is no proven personal “remaining %” JSON analogous to Claude’s `/usage`. Do not guess `cli-chat-proxy`, gRPC-web, or other undocumented RPCs into Kotlin. Discover URLs from the loaded usage page, or parse that page.

## 2. How authentication works

Login is Google / X / Apple / email via `accounts.x.ai`.

Google (and likely Apple) in an **embedded WebView** is the same class of problem as Gemini. Do not use this path for Google-as-IdP on grok.com.

The IdP used in private testing is **X**, not Google. Shipping 1d is the in-app WebView (same CookieManager as the later read). Custom Tabs would put the session in Chrome and cannot feed the native bar.

## 3. What we will and will not do

Shipping (see [[Grok-usage-data-layer]]): in-app WebView, X login, same-origin usage read onto native `4d`.

We will not:

- embed Grok / xAI **Google** login as the intended path
- spoof Chrome UA to force Google or X login (the existing `chromeLikeUserAgent` strip is for Google-as-IdP on third-party sites, already on the WebView)
- call `cli-chat-proxy.grok.com` billing JSON or grok.com gRPC-web from Kotlin
- lift cookies into OkHttp
- poll Grok from WorkManager
- replace board `4d` with Settings → Usage (the refresh sketch’s Tier A)

## Sources

- [Grok FAQ](https://docs.x.ai/grok/faq)
- [Grok overview](https://docs.x.ai/grok/overview)
- [x.ai/pricing](https://x.ai/pricing)
- [https://grok.com/?_s=usage](https://grok.com/?_s=usage) — Eric, live, 2026-09-04
- `20 Research/Notes from Grok Bot/usage-ledger-cursor-supergrok-vs-claude.md` (2026-09-04)
