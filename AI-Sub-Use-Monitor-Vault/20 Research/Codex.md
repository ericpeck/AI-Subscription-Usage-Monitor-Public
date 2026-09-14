---
platform: Codex
board: 4c
researched: 2026-09-04
updated: 2026-09-09
status: researched
---

# Codex usage

**First pass 2026-09-04** (Claude handoff). **Corrected 2026-09-09** against live sources. **Live path 2026-09-09** (Eric). This file is the canonical evidence. If a `Claude outputs/` brief disagrees, **this file wins**.

Rule for the app: [[Codex-usage-data-layer]].

Board `4c` rides the ChatGPT plan. Seed plan line: `Included with Plus · rolling session and weekly caps`. Two windows: Current session / Weekly limit. Vault open question **#8** (Codex vs ChatGPT chat) is still Eric’s call — shipping `4c` reads the Codex/agentic windows, not ChatGPT chat-message caps.

## 1. The data source

### What the meter is

**Confirmed** (OpenAI Help Center, 2026-09-09 scrape of [Using Codex with your ChatGPT plan](https://help.openai.com/en/articles/11369540-using-codex-with-your-chatgpt-plan); Codex pricing pages).

- **5-hour rolling window** and a **weekly cap** above it, exhaustible independently — same relationship as Claude’s session / weekly.
- Local Codex messages **and** cloud tasks draw on those windows.
- Consumption is work-proportional (model, local vs cloud, task size, context, reasoning, speed, tools) — not a fixed messages-per-window count.
- **Credits** are pay-as-you-go after included usage (token-priced since April 2026). Board `4c` has **two** rows. Credits are a third meter and **out of scope** unless Eric grows the board.
- Plus, Pro 5x, Pro 20x, and Business share the 5-hour + weekly structure.
- Codex is **included on Free and Go**; usage limits vary by plan. That **replaces** the 4 Sep claim that Free/Go have no documented windows. Whether those tiers expose session/weekly percentages on the usage page: **REQUIRES LIVE INSPECTION**. Gate Free/Go at sign-in only after that look, same posture as Claude Free.
- Codex, ChatGPT Work, ChatGPT for Excel, and Workspace Agents share one **agentic** allowance when those features exist on the plan. Regular ChatGPT chat, images, and Voice have **separate** limits. That makes open question #8 sharper, not moot: `wham/usage` is the **Codex / agentic** meter, not ChatGPT chat-message limits.
- A full banked Codex reset (referral / promo) refreshes the 5-hour **and** weekly windows and can change the weekly reset date.

### Sanctioned surfaces (human)

**Not** the product UI — do not replace native `4c` with these pages.

- **Start:** [https://chatgpt.com/auth/login](https://chatgpt.com/auth/login) — Eric, 2026-09-09.
- **Read:** [https://chatgpt.com/#settings/Usage](https://chatgpt.com/#settings/Usage) — Eric, 2026-09-09. Canonical for this app. Hash route; after login ChatGPT is chat home, not usage.
- [https://chatgpt.com/codex/settings/usage](https://chatgpt.com/codex/settings/usage) — OpenAI help-center Codex dashboard (fallback).
- [https://chatgpt.com/codex/cloud/settings/analytics](https://chatgpt.com/codex/cloud/settings/analytics) — community write-ups; three meters.

### Public API for a standalone Android app

**None found** for personal ChatGPT-plan Codex windows. Admin / Usage APIs are API-key billing or Enterprise analytics, not Plus session %.

### Undocumented read the CLI already uses

**Confirmed** from OpenAI’s own repo (`openai/codex` issue [#10869](https://github.com/openai/codex/issues/10869), Feb 2026; still current in `codex-rs/backend-client`):

```
GET https://chatgpt.com/backend-api/wham/usage
Authorization: Bearer <access_token>
```

ChatGPT-auth Codex CLI polls this roughly every 60 s (`ChatWidget::prefetch_rate_limits` → `get_rate_limits`). API-key profiles must not poll it (fixed in that issue). Frequent polling is ordinary traffic for OpenAI’s CLI — **not** a reason for AI-SUM to background-poll.

First-party client types (2026-09-09 `openai/codex`) map the payload toward `used_percent`, `resets_at`, and primary / weekly windows, plus optional `additional_rate_limits` and reset-credit metadata. **Do not ship a mapper from those types alone.** Wire JSON keys after a live signed-in response (or a captured page) matches them.

## 2. How authentication works

### Two token sources. Only one is acceptable.

**Source A — Codex CLI OAuth. REFUSED.** PKCE loopback against `auth.openai.com`, public first-party CLI client, tokens in `auth.json` / OS keyring. Using that client ID means AI-SUM presents itself as OpenAI’s first-party software. Device-code (`codex login --device-auth`) is also refused: it ships disabled because OpenAI treats it as a phishing vector. Do not tell users to turn it on.

**Source B — the web session. This is the path (Inferred, NextAuth-shaped; current field names REQUIRES LIVE INSPECTION).**

1. `GET https://chatgpt.com/api/auth/session` — cookie-authenticated (`__Secure-next-auth.session-token`). Returns a session object that includes an access token. Older write-ups: ~2-week lifetime.
2. `GET https://chatgpt.com/backend-api/wham/usage` with `Authorization: Bearer <that token>`.

Both are same-origin on `chatgpt.com`. In-page `fetch()` inside a signed-in WebView is the Claude-shaped method: hop 1 rides the cookie; hop 2 uses what hop 1 returned. No CLI credential, no desktop companion, no cookie lift into OkHttp.

Sources for the two-hop shape date from the `chat.openai.com` era. Confirm the session JSON’s access-token field on a current `chatgpt.com` session before coding it.

### Sign-in

Google SSO in an unmodified embedded WebView is refused (`disallowed_useragent`). Accounts **created via Google** can deadlock if email/password redirects back into that flow.

AI-SUM already strips `; wv` and `Version/4.0 ` (`chromeLikeUserAgent`). Google’s check does not vary by relying party, so the same treatment that works for `claude.ai` / `cursor.com` works for `chatgpt.com`. Standing rule in [[AGENTS]]: normalize UA for Google-as-IdP on third-party sites including chatgpt.com; do **not** use it to sign in to Google first-party (Gemini).

**Codex does not need Custom Tabs.** Custom Tabs would put cookies in Chrome; the later in-page `fetch` would not see them.

Plus on Codex **does** open 1d (unlike Grok Bot). One live session per platform; work is a filing flag, not a second tab.

## 3. What we will and will not do

Shipping: see [[Codex-usage-data-layer]]. The researched method is:

- Reuse the full-screen in-app WebView, UA strip, and child-WebView `window.open`. Do not fork a second sign-in path.
- Issue both hops with `fetch()` inside the WebView on a `chatgpt.com` origin.
- Cache the access token only in the WebView process; refresh by re-reading `/api/auth/session` on 401. Never persist it to `accounts.json`. Never log it.
- Treat session expiry as `NeedsSignIn` → **1h**, same as Claude. Do not auto-loop 1d.
- Isolate behind `UsageReader`. Degrade visibly. Keep last-known-good but never present it as current.
- Map only the two board windows (session + weekly). Drop credits unless the board grows.
- Manual ⟳ and read-on-open only.

We will not:

- use the Codex CLI OAuth client, device-code auth, or any flow that authenticates as Codex CLI
- lift cookies or tokens into OkHttp
- poll in the background
- POST as the user (including redeeming rate-limit reset credits)
- replace `4c` with OpenAI’s usage page
- guess JSON field names without a live payload
- treat this note as a ChatGPT **chat** usage reader

### ToS

OpenAI’s terms are more explicit than Anthropic’s (programmatic extraction; bypassing protective measures). Same posture as Claude: undocumented, isolated, degrade on failure, never claimed as official support. Terms of service go to a lawyer, not to an AI assistant.

## Open items

**REQUIRES LIVE INSPECTION** (still true on device)

1. Whether `#settings/Usage` shows Codex 5-hour/weekly copy, ChatGPT chat limits, or both.
2. Current `GET /api/auth/session` access-token field name on this account.
3. Live `wham/usage` vs the CLI types (primary vs weekly; extra model limits).
4. Whether Free/Go return session/weekly percentages, or error / omit windows.

**For Eric**

5. Open question #8 — confirm `4c` is Codex / agentic usage, not ChatGPT chat limits.
6. Credits stay off `4c`, or grow a third row.

## Sources

- [openai/codex issue #10869](https://github.com/openai/codex/issues/10869) — `backend-api/wham/usage`, ~60 s poll, ChatGPT `auth.json` token
- `openai/codex` `codex-rs/backend-client/src/client/rate_limit_resets.rs` (checked 2026-09-09) — ChatGPT path still `{base}/wham/usage`
- [Using Codex with your ChatGPT plan — OpenAI Help Center](https://help.openai.com/en/articles/11369540-using-codex-with-your-chatgpt-plan) — usage dashboard URL; Free/Go included; shared agentic pool vs ChatGPT chat/images/Voice; banked reset refreshes both windows
- [Codex Pricing](https://chatgpt.com/codex/pricing/) / [learn.chatgpt.com pricing](https://learn.chatgpt.com/docs/pricing)
- [How to Check Codex Usage in ChatGPT](https://www.jdhodges.com/blog/how-to-check-codex-usage-chatgpt-plus/) — alternate analytics URL, three meters (updated 2026-09-06)
- [acheong08/ChatGPT Authentication wiki](https://github.com/acheong08/ChatGPT/wiki/Authentication) — `/api/auth/session`, `__Secure-next-auth.session-token` (older `chat.openai.com` era)
- [OpenAI Terms of Use](https://openai.com/policies/row-terms-of-use/)
- Historical brief: `Claude outputs/codex-handoff-for-grok.md` (4 Sep 2026; claims corrected 9 Sep 2026)
- Board `4c` / outline: Codex rides the ChatGPT plan
