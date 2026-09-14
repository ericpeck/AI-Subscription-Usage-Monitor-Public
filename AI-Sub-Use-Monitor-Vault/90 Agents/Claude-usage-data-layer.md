# Claude usage — data layer (2026-09-04)

Evidence file: [[Claude]] (`20 Research/Claude.md`, researched 2026-09-04). This note is the rule. Claude only.

**Eric's call (same night):** recreate the competitor's method, **Claude first**. The other eight platforms stay on fake board data until each has its own note and adapter. They will not share Claude's URLs.

The live path is still **undocumented and ToS-exposed**. Isolate it, degrade on 403, do not pretend it is a public API. Terms of service still go to a lawyer.

## Sanctioned surfaces (human)

claude.ai shows the user's own session and weekly windows. Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class: a window only needs a percentage and a reset time.

Anthropic's Analytics / Rate Limits APIs are Platform API-key and enterprise admin. They do **not** report Pro/Max consumer windows.

## What Android apps actually do

No public consumer Usage API. Trackers use the `claude.ai` web session.

Inferred endpoints (browser-extension ecosystem; not official):

| Endpoint | Returns |
|---|---|
| `GET https://claude.ai/api/organizations` | org list; take the UUID |
| `GET https://claude.ai/api/organizations/{org_uuid}/usage` | `five_hour`, `seven_day`, `seven_day_opus` |
| `GET https://claude.ai/api/organizations/{org_uuid}/subscription_details` | plan name, next charge date |

Each usage window carries `utilization` (0–100) and `resets_at` (ISO 8601). That maps to board `4a` (Current session / Weekly limit). Skip `seven_day_opus` unless the board grows a third row. Board `4a` paints **Max 5×** as sample copy; live `4a` / `1f` Active must show the account’s real tier. `subscription_details` often has billing status and no plan label — take `rate_limit_tier` (and similar) from the org object (`default_claude_ai_pro` → **Pro**, max → **Max 5×** / **Max 20×**). Do not keep “Signed in” when that tier is known.

Auth is cookies only: `sessionKey` plus `lastActiveOrg`. Android sandboxing means the official Claude app's session is unreadable. In-app WebView sign-in is the only stock-Android route.

## The method that works vs the one that died

The shipping competitor signs the user in **inside a WebView** and keeps the session there.

Claude-Counter-Android used the same cookies from a **background native HTTP client** and got **HTTP 403** after Anthropic's April 2026 block. The block is inferred as automation fingerprinting, not invalid cookies.

**Hard rules for AI-SUM:**

- Issue `/usage` with `fetch()` **inside the WebView**, on a document whose origin is `claude.ai`. A blank WebView cannot `fetch` that origin (CORS). CookieManager alone is not enough for refresh.
- **Do not open Chrome Custom Tabs or the system browser for sign-in.** Those cookies live in Chrome. 1d is a full-screen **in-app** WebView so Google OAuth and the later `fetch` share one CookieManager. Handle `window.open` with a child WebView, not by loading the popup into the parent (that drops `window.opener` and sticks after Google).
- **Do not lift cookies into OkHttp** (or any native client). That reintroduces the 403.
- **Do not POST** as the user (no Auto-Wake / Haiku message). Read only. Writes are full account authority and the trust problem.
- **Never log cookies**, never put them in crash reports or `adb`.
- No WorkManager polling of `claude.ai/api` this slice. Manual ⟳ and read-on-open only. Aggressive background polling is what triggered the crackdown. **Do not replace 4a with Claude’s own Settings → Usage page** (the user-initiated-refresh sketch’s Tier A). That page can be a fallback if parse fails; the product UI is still native fill + hairline.
- Detect **Free** at login (Claude dropped Free usage data ~21 Aug 2026). Say so immediately.
- Session expiry is a first-class state. Keep last-known-good but **do not present it as current**. On `NeedsSignIn` from ⟳ or read-on-open, mark `needsReauth` and stay on **1h**. Do not auto-open 1d. If the user backs out of 1d, stay on 1h. A generic `Failed` read does not open 1d. **Current session** must remain on `4a` when `/usage` has weekly data but omits or partially fills `five_hour` (0% used is valid; unstarted window uses `Resets in 5 hr`).
- `forgetSession()` (`CookieManager.removeAllCookies` + WebStorage) runs when **Remove** deletes the last live Claude **or SuperGrok** tab. Removing Claude while SuperGrok stays expires claude.ai cookies and site data only. Removing Grok while Claude stays expires Grok/X hosts only. 1d drops that platform’s cookies before the sign-in URL loads.
- Outline §8.1 `parse(page: String)` of HTML is the **wrong shape** for Claude. The payload is JSON from `/usage`.

Claude Code OAuth (`GET https://api.anthropic.com/api/oauth/usage`, `User-Agent: claude-code/…`) is desktop-only. Token lives in the Claude Code credential store and expires ~hourly. **Out for the phone app.**

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4a` figures | Still used if someone calls `FakeAccountStore.add(claude)` in tests |
| Manual entry of dashboard percentages | First-class when we build §8.3 |
| Official public Usage API | Not available — revisit |
| Claude Code OAuth on phone | Out |
| Undocumented `/usage` via OkHttp + stolen cookies | Out (403) |
| Undocumented `/usage` issued inside a signed-in WebView | **Shipping for Claude** — 1d is a **full-screen in-app WebView** (not Chrome Custom Tabs: those cookies never reach the in-page `fetch`). Google OAuth `window.open` gets a child WebView in the same CookieManager. After a signed-in `claude.ai` path (`/` included; not `/login`) the app conceals Claude’s shell, auto-reads, and returns to 4a. Continue is a header fallback. A failed Continue must not block the later auto-read. Undocumented. Isolate behind `UsageReader`. SuperGrok is a separate reader ([[Grok-usage-data-layer]]). The other seven stay fake. |

**Auto-Wake / Haiku is out** unless Eric reopens it. Do not POST as the user.

Package 16 still gates Gemini, Codex, Perplexity, Cursor, Copilot, M365, and Grok Bot. SuperGrok is [[Grok-usage-data-layer]]. This note does not authorize guessing those other endpoints.
