---
platform: Claude
board: 4a
researched: 2026-09-04
updated: 2026-09-04
status: researched
aliases:
  - ai-usage-teardown
---

# Teardown: How Android usage-tracker apps read Claude usage

Reference for the AI Subscription Usage Monitor project. **Researched 2026-09-04.** Rule for the app: [[Claude-usage-data-layer]].

Two subjects:

- **AI Usage: Claude & Gemini** — closed source, package `u.sage`, developer "Summoner's Rift". Live competitor.
- **Claude-Counter-Android** — open source (`ignitedvisions/Claude-Counter-Android`). Deprecated, but its failure is the useful part.

Evidence is labeled **Confirmed** (stated by the developer or the listing) or **Inferred** (deduced from the wider tooling ecosystem, consistent with observed behavior).

---

## 1. The data source

There is no public API for consumer subscription usage. Anthropic publishes usage APIs only for Platform API keys (Admin/Analytics/Cost) and enterprise admin. Neither reports Pro/Max plan windows. Every tracker therefore uses one of two undocumented surfaces.

### Surface A — claude.ai web session (what the Android apps use)

**Inferred endpoints.** The browser-extension ecosystem converges on the same three:

| Endpoint | Returns |
|---|---|
| `GET https://claude.ai/api/organizations` | org list; take the UUID |
| `GET https://claude.ai/api/organizations/{org_uuid}/usage` | `five_hour`, `seven_day`, `seven_day_opus` |
| `GET https://claude.ai/api/organizations/{org_uuid}/subscription_details` | plan name, next charge date |

Each usage window carries `utilization` (0–100) and `resets_at` (ISO 8601). This maps exactly onto the competitor's advertised fields: current session (5-hour), weekly limits, and a separate Claude Design quota.

**Auth.** Cookies only — `sessionKey` plus `lastActiveOrg`. No API key, no token paste.

### Surface B — Claude Code OAuth (desktop tools only)

```
GET https://api.anthropic.com/api/oauth/usage
Authorization: Bearer <oauth_access_token>
anthropic-beta: oauth-2025-04-20
User-Agent: claude-code/<version>
```

Returns `five_hour`, `seven_day`, `seven_day_opus`, `seven_day_sonnet`, `extra_usage`. This is server-side ground truth and is stable at ~180 s polling.

The `User-Agent` header is load-bearing. Omit it and requests fall into an aggressively rate-limited bucket that returns persistent 429s.

**Why this is unavailable to you.** The token lives in the Claude Code credential store on a desktop (`~/.claude/.credentials.json`, or macOS Keychain under `Claude Code-credentials`) and expires roughly hourly, refreshed by the Claude Code process. There is no Claude Code on Android and no OAuth client registration a third-party mobile app can obtain. Reachable only if you build a desktop companion that relays.

---

## 2. How the competitor authenticates

**Confirmed.** The app opens an in-app **WebView** and has the user sign in to each provider inside it. The developer states this directly in a review reply:

> "if we used an external browser, its session wouldn't link to the app's WebView, so we wouldn't be able to authenticate and track your usage"

And on why it cannot reuse the official Claude app's session:

> "Android's security prevents our app from reading data inside the official Claude app, so a direct login is required to fetch your usage limits."

That second point is correct and worth internalizing: Android app sandboxing means the official Claude app's cookies and tokens are unreadable by any other app. A WebView login is not laziness; it is the only route on stock Android without root or an Accessibility Service.

**The resulting flow (inferred, standard for this pattern):**

1. WebView loads `https://claude.ai/login`; user completes sign-in, including any 2FA.
2. Android `CookieManager` persists `sessionKey` and `lastActiveOrg` in the app's private WebView cookie store.
3. The app discovers the org UUID once and caches it.
4. It polls `/usage` with those cookies attached — either by injecting `fetch()` into the WebView, or by lifting cookies from `CookieManager` into an OkHttp call.
5. Response percentages and `resets_at` drive the dashboard, widget, notification, and calendar entries.

**Confirmed side effect:** Auto-Wake Up "sends a Haiku message at reset." That is a write, not a read — it POSTs a real message to claude.ai as the user. The app therefore holds full account authority, not read-only usage scope.

---

## 3. Why the open-source app died and this one did not

Claude-Counter-Android used the same cookie-plus-`/usage` approach from a **background service**. Its README records the outcome:

> Anthropic "has actively blocked third-party and automated access" as of April 2026. "Direct fetches using session cookies from background services now result in HTTP 403 Forbidden errors."

It now detects 403, stops polling, shows a warning banner, and serves cached data. Its README redirects users to a browser extension using SSE stream interception.

**Inferred explanation of the difference.** The block is not cookie validation — it is automation fingerprinting. A bare OkHttp call from a background service presents a non-browser User-Agent, a non-browser TLS/JA3 fingerprint, no `sec-ch-ua` client hints, no `Referer`, and no browser-shaped header order. A request originating inside a WebView presents a genuine Chromium fingerprint on every axis. Third-party desktop trackers reach the same conclusion from the other direction: they succeed by spoofing "Chrome 131 TLS fingerprints."

**Consequence for your design:** the WebView is not just the login mechanism. It is the request-issuing mechanism. Issuing the `/usage` call from inside the WebView context is what keeps it working. Lifting the cookie out to a native HTTP client reintroduces the 403 risk.

---

## 4. Refresh mechanics

Confirmed from the listing and review replies:

- Manual refresh from a notification action ("Quick Refresh").
- Persistent lock-screen notification carrying current stats.
- Home screen widgets per service, resizable, multiple instances.
- Reset times written to Google Calendar (separate Google OAuth).
- Scheduled action at reset time to fire the Haiku wake-up message.

Polling interval is not published. Sessions expire silently — a reviewer saw a Copilot reset date frozen at "1 Mar" and the developer's diagnosis was "your background session expired. Logging in again refreshes it," adding that the app "clearly need[s] a 'session expired' UI." Treat that as a confirmed defect class, not an edge case.

Reference point for pacing: community tooling treats ~180 s as safe against the OAuth endpoint. Against `claude.ai/api`, slower is safer, because aggressive background polling is what triggered the April 2026 crackdown.

---

## 5. Permissions and data handling

| Item | Status |
|---|---|
| Play Data Safety | No data collected, no data shared with third parties |
| Required permissions | None declared |
| Stated storage | "All data stays locally on your device"; "Session cookies and usage data never leave your phone" |
| Independent verification | None. Closed source, no published privacy audit. |

The privacy claims are plausible for this architecture — the design does not need a server — but they are unverifiable assertions.

---

## 6. Review complaints, grouped

Rating 4.4 across 102 reviews, 10,000+ installs, last updated 2026-08-22.

**Trust in the WebView login — the dominant theme.** Three separate reviewers refused or objected to entering provider credentials inside a third-party app. One asked outright: "Are you trying to collect access credentials from other people?"

**One unresolved security report (June 15, 2026).** A reviewer stated that after signing into Google and ChatGPT through the app, an unauthorized Pixel 7 appeared as a signed-in device on both Google accounts, forcing password changes and session revocation. The developer's public reply thanks the user and says "the fix is definitely locked in for the next update" — but never explains what the defect was. Whatever the cause, a confirmed-then-patched auth defect existed. Verify independently before drawing conclusions; treat it as the single most damaging item in the listing.

**Free tier no longer works (~2026-08-21).** Two reviewers hit "No Usage Data Available" the same morning. Developer: "Claude stopped providing usage data for Free plan accounts." Claude tracking now requires Pro or Max.

**Silent session expiry.** Stale reset dates with no error state. Fix is a full re-login; the app does not say so.

**No multi-account support.** A user with work and personal Copilot accounts could not add both, called it "doesn't do what it says on the tin," and uninstalled.

**Slow or blank first login.** WebView login initially renders blank and is "damn slow."

**Auto-Wake Up is blunt.** No quiet hours, so it fires overnight. It also opens a new thread each time instead of reusing one.

**Cosmetic and UX.** Widget hard to find in the picker on Pixel 10; inconsistent session/weekly ordering between services; requests for count-down ("65% remaining") instead of count-up; requests for Cursor, Grok, NotebookLM, OpenCode.

---

## 7. Implications for this project

1. **WebView cookie capture is the only viable Android path for Claude today.** Plan for it. Reuse of the official Claude app's session is impossible under Android sandboxing.
2. **Issue requests from inside the WebView.** Do not lift cookies into OkHttp. That is the difference between the shipping app and the dead one.
3. **Build the session-expiry state first.** It is the competitor's most common functional complaint and the cheapest thing to beat them on.
4. **Gate on plan tier at login.** Detect Free and say so immediately, rather than showing "No Usage Data Available."
5. **Wear OS is the open gap.** The competitor ships widgets and notifications, no watch surface.
6. **Multi-account is an unmet, explicitly requested need.** The developer calls it "a highly challenging problem" and has deferred it.
7. **Do not implement an auto-wake write action.** It converts a read-only monitor into an app with full account write authority. That is where the trust objections concentrate, and it is the feature most likely to be read as automated abuse of a subscription.
8. **Assume the endpoints will break.** These are undocumented and actively defended. April 2026 blocked background fetches; August 2026 removed Free plan data. Isolate the fetch layer behind one interface, cache last-known-good, and ship a visible degraded state.

### Terms of Service exposure

Anthropic moved against third-party access twice in 2026 and now restricts subscription usage through unauthorized third-party harnesses. Reading `/usage` with your own session cookie is far milder than proxying inference, and the competitor has survived on the Play Store since at least March 2026. But this is undocumented-endpoint territory with an active enforcement posture. Design so that a shutdown degrades your app rather than ending it.

---

## Sources

- [AI Usage: Claude & Gemini — Google Play](https://play.google.com/store/apps/details?id=u.sage) (listing, Data Safety, and full review thread)
- [Claude-Counter-Android — GitHub](https://github.com/ignitedvisions/Claude-Counter-Android)
- [Claude Usage Tracker v2.1.0 — GitHub](https://github.com/sshnox/Claude-Usage-Tracker-v2.1.0) (endpoint and field names)
- [claude-token-tracker — GitHub](https://github.com/Krabby24/claude-token-tracker) (SSE interception alternative)
- [ClaudeMeter: Claude usage tracker gone](https://claude-meter.com/t/claude-usage-tracker-gone) (endpoint list, TLS fingerprinting)
- [Claude-Code-Usage-Monitor issue #202](https://github.com/Maciek-roboblog/Claude-Code-Usage-Monitor/issues/202) (OAuth usage API)
- [Anthropic cracks down on unauthorized third-party harnesses — VentureBeat](https://venturebeat.com/technology/anthropic-cracks-down-on-unauthorized-claude-usage-by-third-party-harnesses)
- [Analytics API](https://platform.claude.com/docs/en/manage-claude/analytics-api) and [Rate Limits API](https://platform.claude.com/docs/en/manage-claude/rate-limits-api) — Claude Platform Docs (official, API-key scope only)
