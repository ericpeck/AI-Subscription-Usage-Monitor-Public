# Cursor usage — data layer (2026-09-09)

Evidence file: [[Cursor]] (`20 Research/Cursor.md`). This note is the rule.

**Eric's call (2026-09-09):** SuperGrok shape. Native board `4f` stays. In-app WebView to the official spending page. Do not Custom Tabs. Do not `api2.cursor.sh`.

The live path is still **undocumented and ToS-exposed**. Isolate it, degrade on 401/403, do not pretend it is a public API.

## Sanctioned surfaces (human)

- **Start / login:** [https://cursor.com/dashboard](https://cursor.com/dashboard). Google-as-IdP on cursor.com (identity provider used in private testing). Same class as Claude, **not** Gemini.
- **After login:** [https://cursor.com/agents](https://cursor.com/agents) is signed-in home, not usage.
- **Read:** [https://cursor.com/dashboard/spending](https://cursor.com/dashboard/spending) (Settings at the bottom of the product). June 2026 teams post: included / Cursor models vs third-party API models — the same split as board `4f` when the page still shows two pools.
- **IDE:** Settings usage summary (Usage Summary = Always).

Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class.

There is no official personal “remaining %” API analogous to Claude’s `/usage`.

## What we will not do

- call `POST https://api2.cursor.sh/aiserver.v1.DashboardService/GetCurrentPeriodUsage` or related DashboardService RPCs
- lift cookies into OkHttp
- read Cursor’s local SQLite / state DB
- start 1d on `/dashboard/spending` (unauthenticated spending is the wrong sheet)
- auto-read unauthenticated `/dashboard` or `/agents` with no OAuth previous URL (Grok Account-sheet class of bug). Post-SSO `/agents` after Google / GitHub / Apple / authenticator **is** auto-read — same as grok.com chat after X.
- Chrome Custom Tabs (those cookies never reach the in-page `fetch`)
- UA tricks to sign in to Google **first-party** surfaces (Gemini). The existing `chromeLikeUserAgent` strip is for Google-as-IdP on cursor.com.

## The method that ships

Same posture as [[Grok-usage-data-layer]]:

- 1d is a full-screen **in-app** WebView. Start at `https://cursor.com/dashboard`. Same `CookieManager` as Claude and SuperGrok. Handle `window.open` (Google) with a child WebView. After return to `cursor.com/agents`, Continue / ⟳ / read-on-open load `https://cursor.com/dashboard/spending`. Conceal only while reading.
- Auto-read when the path is `/dashboard/spending`, **or** when cursor.com appears right after Google / GitHub / Apple / authenticator.cursor.sh (post-SSO `/agents` is signed-in; the reader then loads spending). Continue remains the header fallback.
- Read with in-page JS after that spending page hydrates. Prefer same-origin JSON the page already fetched (`performance` resource URLs matching spending/usage/billing/quota — **same origin as cursor.com**, never `api2.cursor.sh`). Fallback: parse the official spending UI. Plan comes from **CURRENT PLAN** (Pro+ / Pro / Ultra), not from the Upgrade card — observed during private testing: CURRENT PLAN Pro+ with an Ultra upsell (`Exibit1.png`). If the page has two labeled pools, two `4f` windows (Cursor Models / Other Models). If it is one pool, one fill — do not invent a second window. **Do not put Grok Bot on `4f`.** The spending page’s **Grok Bot → Weekly usage** row maps onto native `4i` (see [[Grok-bot-usage-data-layer]]). Monthly copy `Usage limits reset on Sep 18` is the Cursor hairline; Grok Bot’s `Resets Sep 13` is `4i` only.
- **Do not lift cookies into OkHttp.** **Do not POST.** No WorkManager poll.
- `NeedsSignIn` marks `needsReauth` and stays on **1h**. A generic `Failed` read does not open 1d.
- Remove Cursor expires cursor.com / authenticator.cursor.sh cookies and site data only. Do **not** clear Google / Apple / GitHub when removing Cursor (Claude still needs Google-as-IdP). Full jar wipe only when the last live WebView-session tab is gone.

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4f` figures | Still used if someone calls `FakeAccountStore.add(cursor)` in tests |
| Manual entry of dashboard percentages | First-class when we build §8.3 |
| Official public consumer Usage API | Not available — revisit |
| Undocumented DashboardService / cookie lift | Out |
| In-WebView Google login on cursor.com, then same-origin spending read | **Shipping for Cursor** — 1d full-screen in-app WebView. Undocumented. Isolate behind `UsageReader`. |

**Auto-Wake is out.** Do not POST as the user.
