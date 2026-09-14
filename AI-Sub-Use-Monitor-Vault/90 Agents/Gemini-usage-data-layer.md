# Gemini usage — data layer (2026-09-04)

Evidence file: [[Gemini]] (`20 Research/Gemini.md`, researched 2026-09-04). This note is the rule.

**Eric’s call (2026-09-09):** Gemini is **off the add list**. Do not plus a fake `4b`. Research stays; do not restore the row unless Eric asks.

Research only. **Do not implement a Gemini parser, WebView login, or unofficial RPC client from this note.**

## Sanctioned surfaces (human)

- Gemini Apps Usage Limits (Google support doc, 2026-09-04 pass).
- In-product settings on `gemini.google.com`. Exact deep link still needs live inspection.

Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class.

## What does not work

- **In-app WebView Google sign-in.** Google blocks embedded WebViews (`disallowed_useragent`). Do not strip `; wv` / spoof Chrome to get around it.
- **Cookie lift from Chrome Custom Tabs.** CCT cookies live in Chrome, not in our WebView `CookieManager`. There is nothing to `fetch()` same-origin.
- **Background poll.** Same reason as Claude: silent harvest is what died.

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4b` figures (this slice) | Tests only — not offered on 1b / 1f |
| Manual entry of dashboard percentages | First-class when we build §8.3 |
| Official public consumer Usage API | Not available — revisit |
| Chrome Custom Tabs to Gemini’s own usage page | Allowed later as a **handoff**, not as a native bar feed |
| Embedded WebView login / `batchexecute` / cookie scrape | Out |

The user-initiated-refresh sketch’s **Tier A for Gemini** (open their page in CCT on ↻) is compatible with this rule. It is **not** a substitute for board `4b` native bars. Do not block v1 on Gemini parse.

Package 16 still waits on a live deep-link confirmation before any Gemini shell is built.
