---
platform: Gemini
board: 4b
researched: 2026-09-04
updated: 2026-09-09
status: researched
aliases:
  - gemini-webview-findings
---

# Gemini usage

**Researched 2026-09-04.** Rule for the app: [[Gemini-usage-data-layer]]. **Off the add list** (Eric, 2026-09-09).

Source for this pass: vault sketch `usage-ledger-user-initiated-refresh.md` (same night as the Claude teardown). Exact in-product usage URL still **requires live inspection**. Do not guess `batchexecute` or other undocumented RPCs into the app.

## 1. The data source

### Sanctioned surfaces (human)

- Gemini Apps **Usage Limits**: [support.google.com/gemini/answer/16275805](https://support.google.com/gemini/answer/16275805).
- In-product settings on `gemini.google.com` (and related Google AI surfaces). The deep link is not confirmed in this pass.

These are the official places a user reads their own numbers. Linking there, or letting them type what they see, is a sanctioned product path.

### Public API for a standalone Android app

**None found.** There is no proven consumer “remaining %” JSON analogous to Claude’s `/usage`. Google’s Platform / AI Studio APIs are not the Gemini Apps subscription windows on board `4b`.

## 2. How authentication works

Google Account sign-in in an **embedded WebView** is blocked (`disallowed_useragent`). Claude-style “log in inside our WebView, then `fetch()` same-origin” **does not port**. Chrome’s cookie jar is not readable from the app.

The conservative shell, if we ever show Gemini’s own usage UI, is **Chrome Custom Tabs** (AndroidX Browser). That is a handoff, not a session we can reuse for a native `/usage` read.

## 3. What we will and will not do

We will not:

- embed Gemini login in a WebView
- spoof Chrome UA to force Google login in a WebView
- scrape Chrome cookies or `batchexecute`
- poll Gemini from WorkManager

Stay on fake `4b` board data until a later pass confirms a deep link and Eric picks Custom Tabs vs manual entry.

## Sources

- [Gemini Apps limits](https://support.google.com/gemini/answer/16275805)
- `AI-Sub-Use-Monitor-Vault/usage-ledger-user-initiated-refresh.md` (2026-09-04)
