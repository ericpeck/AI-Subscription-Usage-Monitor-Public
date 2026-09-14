# Codex usage — data layer (2026-09-09)

Evidence file: [[Codex]] (`20 Research/Codex.md`). This note is the rule.

**Eric's call (2026-09-09):** same in-app WebView as Claude / SuperGrok / Cursor. Native board `4c` stays. Start at ChatGPT login. Read usage from ChatGPT Settings → Usage. Do not Custom Tabs. Do not Codex CLI OAuth / device-code.

The live path is still **undocumented and ToS-exposed**. Isolate it, degrade on 401/403, do not pretend it is a public API.

## Sanctioned surfaces (human)

- **Start / login:** [https://chatgpt.com/auth/login](https://chatgpt.com/auth/login) (Eric, 2026-09-09). Google-as-IdP on chatgpt.com is allowed with the existing UA strip. Same class as Claude / Cursor, **not** Gemini.
- **Read:** [https://chatgpt.com/#settings/Usage](https://chatgpt.com/#settings/Usage) (Eric, 2026-09-09). Hash route on chatgpt.com — not a separate origin. After login ChatGPT lands on chat (`/`), which is signed-in home, not usage.
- Help-center Codex page (`/codex/settings/usage`) and community analytics URL remain fallbacks if the hash sheet is missing. Do not replace native `4c` with any of these pages.

Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class.

There is no official personal “remaining %” API for ChatGPT-plan Codex windows. Admin / Usage APIs are API-key billing, not Plus session %.

## What the meter is

Board `4c`: **Current session** (5-hour) and **Weekly limit**. Credits are a third meter with no board row — drop them. Extra model limits (`additional_rate_limits`) stay off `4c`.

OpenAI’s Codex client types `GET /backend-api/wham/usage` as `plan_type` plus `rate_limit.primary_window` / `secondary_window`, each with `used_percent`, `limit_window_seconds`, `reset_after_seconds`, `reset_at` (unix). Classify windows by duration (≈5 hours vs ≈7 days), not only by primary/secondary names. ChatGPT chat / images / Voice are a different surface; prefer the Codex/agentic windows. `#settings/Usage` is the human page Eric named — parse it when the JSON hop is empty.

## What we will not do

- Codex CLI OAuth client, `auth.json`, or device-code login
- lift cookies or bearer tokens into OkHttp
- persist the session access token in `accounts.json`
- POST as the user (including redeeming rate-limit reset credits)
- Chrome Custom Tabs
- UA tricks to sign in to Google **first-party** surfaces (Gemini)
- background polling (`wham/usage` every 60 s is what Codex CLI does — not AI-SUM)
- replace `4c` with OpenAI’s usage page
- auto-read `/auth/login`

## The method that ships

Same posture as [[Cursor-usage-data-layer]]:

- 1d is a full-screen **in-app** WebView. Start at `https://chatgpt.com/auth/login`. Same `CookieManager`. Handle `window.open` (Google / Apple / Microsoft) with a child WebView. After a signed-in chatgpt.com path (not `/auth/login`), Continue / ⟳ / read-on-open load `https://chatgpt.com/#settings/Usage`. Conceal only while reading.
- Auto-read when the URL hash is `#settings/Usage` (or `/codex/settings/usage`), **or** when chatgpt.com appears right after Google / Apple / Microsoft / `auth.openai.com` / ChatGPT’s own login path (post-login chat is signed-in; the reader then loads the usage hash). Unauthenticated `/auth/login` is not auto-read. Continue remains the header fallback.
- Read with in-page JS on a `chatgpt.com` (or `chat.openai.com`) origin. Hop 1: `GET /api/auth/session` (cookie). Hop 2: `GET /backend-api/wham/usage` with that bearer — never log the token, never send it to Kotlin. Also reuse same-origin JSON the settings page already fetched. Fallback: parse the Usage sheet (5-hour / weekly / `% used`). Keep **Current session** on `4c` when only weekly is present (idle 5-hour row).
- Plan from `plan_type` / the sheet (Plus, Pro, Business, …). `Included with Plus` is board-fake copy; live is `{Plan} · rolling session and weekly caps`. Free/Go: show windows if they exist; refuse only when that tier has no session/weekly figures.
- **Do not lift cookies into OkHttp.** **Do not POST.** No WorkManager poll.
- `NeedsSignIn` marks `needsReauth` and stays on **1h**. A generic `Failed` read does not open 1d.
- Remove Codex expires chatgpt.com / chat.openai.com / auth.openai.com cookies and site data only. Do **not** clear Google / Apple / Microsoft when removing Codex. Full jar wipe only when the last live WebView-session tab is gone.

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4c` figures | Still used if someone calls `FakeAccountStore.add(codex)` in tests |
| Manual entry of dashboard percentages | First-class when we build §8.3 |
| Official public consumer Usage API | Not available — revisit |
| Codex CLI OAuth / cookie lift | Out |
| In-WebView ChatGPT login, then same-origin usage read | **Shipping for Codex** — 1d full-screen in-app WebView. Undocumented. Isolate behind `UsageReader`. |

**Auto-Wake is out.** Do not POST as the user.
