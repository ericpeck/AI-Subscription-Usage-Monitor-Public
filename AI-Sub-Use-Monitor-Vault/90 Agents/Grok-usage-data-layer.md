# Grok usage — data layer (2026-09-04)

Evidence file: [[Grok]] (`20 Research/Grok.md`, researched 2026-09-04). This note is the rule. **SuperGrok / grok.com only** — not Grok Bot (`4i`). See [[Grok Bot]].

**Eric's call (2026-09-04):** SuperGrok next, same shape as Claude. Native board `4d` stays. Do not ship grok.com Usage as the product UI (Tier A). Do not Custom Tabs.

The live path is still **undocumented and ToS-exposed**. Isolate it, degrade on 401/403, do not pretend it is a public API. Terms of service still go to a lawyer.

## Sanctioned surfaces (human)

- SuperGrok Settings → Usage on [grok.com](https://grok.com).
- [https://grok.com/?_s=usage](https://grok.com/?_s=usage) — **confirmed live** in private testing (2026-09-04, X as IdP).

Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class: a window only needs a percentage and a reset time.

xAI developer RPS/TPM and Management billing are **not** the consumer SuperGrok weekly pool. There is no official personal “remaining %” API analogous to Claude’s `/usage`.

## What the meter is

One **shared weekly** compute pool (Voice / Chat / Imagine / Build / API on the Usage tab). Board `4d` paints that week as a **segmented** Voice / Chat / Coding bar — design of record, not three APIs. Map Build → Coding. Extra products (Imagine, API) may appear as extra greys. If only a total % exists, one fill, no segments. **0% used is valid SuperGrok**, not Free.

## The method that ships

Same posture as [[Claude-usage-data-layer]]:

- 1d is a full-screen **in-app** WebView. Start at xAI sign-in: `https://accounts.x.ai/sign-in?redirect=grok-com&return_to=%2F%3Fq%3D%26reasoningMode%3Dnone%26voice%3Dfalse` (Eric, 2026-09-04 — grok.com home is chat + cookie banner, not login). Same `CookieManager` as Claude. Handle `window.open` (X / Google) with a child WebView, not Chrome Custom Tabs. After return to grok.com, Continue / ⟳ / read-on-open load `https://grok.com/?_s=usage`. Do not open `/?_s=usage` as the 1d start — before a session that URL is an Account sheet (Language / Birth Year).
- Read with in-page JS after that usage page loads. **Wait until the Usage sheet is on the page** (`Weekly SuperGrok Limit` / `% used`) — `onPageFinished` plus a short settle is not enough after X SSO (2026-09-09: Continue showed a blank grok.com, then the sheet, while we already reported Failed). Prefer same-origin JSON the page already fetched (`performance` resource URLs matching usage/credit/billing/quota, `GET` + `credentials: 'include'`). A 401/403 on **one** of those URLs is not session death after X login: try the rest, then parse the sheet, then `needs_sign_in` only if the sheet had nothing. Fallback: parse the official Usage UI. Outline §8.1 `parse(page)` is the fallback shape here, not a silent scrape of some other origin. Sheet copy to match: headline `N% used` (not the first `%`, which is often Voice), Voice/Chat/Coding percents that sum to that headline, `Resets September 10, 2026 at 3:01 PM` (not ISO). Grok stays **weekly-only**.
- **Do not** guess `cli-chat-proxy`, gRPC-web, or undocumented REST paths into Kotlin.
- **Do not lift cookies into OkHttp.** **Do not POST.** No WorkManager poll. Manual ⟳ and read-on-open only.
- Auto-read when the URL is grok.com **and** (`?_s=usage` **or** the previous host was X / Google / Apple / `accounts.x.ai`). After X SSO the redirect is grok.com **chat**, not the usage query — that landing is signed-in; the reader then loads `/?_s=usage`. Do **not** auto-read grok.com home with no OAuth previous, and do **not** start 1d on `/?_s=usage` (before a session that URL is an Account sheet). A silent failed read must not clear the URL latch or 1d will overlay-loop. Concealing grok.com’s own shell happens **while reading**. Continue remains the header fallback.
- `chromeLikeUserAgent` is already on the WebView (Google-as-IdP on third-party sites). Do **not** add extra UA spoofing for X, and do not use UA tricks to sign in to Google first-party surfaces.
- Free / no weekly pool: refuse with a clear message if detectable. Do not treat 0% as Free.
- Session expiry is per platform. Claude expiry does not flag SuperGrok, and vice versa. `NeedsSignIn` marks `needsReauth` and stays on **1h** — it does not auto-open 1d. Reauth is the user’s Sign in / Log in again. A generic `Failed` read does not open 1d.
- `forgetSession()` (`CookieManager.removeAllCookies` + WebStorage) runs when **Remove** deletes the last live Claude **or SuperGrok** tab. Removing Grok while Claude stays expires grok.com / accounts.x.ai / x.com cookies and site data only. Removing Claude while SuperGrok stays expires claude.ai only. 1d also drops that platform’s cookies before loading the sign-in URL, so a leftover session cannot skip login. Do not clear Google / Apple cookies when removing Grok.

Community gRPC-web `GetGrokCreditsConfig` / CLI OAuth is **out** (ToS; different credential than grok.com cookies).

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4d` figures | Still used if someone calls `FakeAccountStore.add(grok)` in tests |
| Manual entry of dashboard percentages | First-class when we build §8.3 |
| Official public consumer Usage API | Not available — revisit |
| Chrome Custom Tabs to SuperGrok Usage | Out for the native bar. Allowed later as a **handoff** only |
| Embedded WebView Google login | Out |
| Undocumented billing-proxy / gRPC-web / cookie lift | Out |
| In-WebView X login then same-origin usage read | **Shipping for SuperGrok** — 1d full-screen in-app WebView, usage URL confirmed. Undocumented. Isolate behind `UsageReader`. |

**Auto-Wake is out.** Do not POST as the user.

This note does not authorize Cursor, Codex, Gemini, Grok Bot, or the other remaining fakes.
