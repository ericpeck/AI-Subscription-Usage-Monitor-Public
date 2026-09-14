# Grok Bot usage — data layer (2026-09-09)

Evidence file: [[Grok Bot]] (`20 Research/Grok Bot.md`). This note is the rule. **Not SuperGrok.** grok.com Usage stays on [[Grok-usage-data-layer]] / board `4d`.

**Eric’s call (2026-09-09):** Grok Bot billed through Cursor is already on `cursor.com/dashboard/spending` (Exhibit1: Weekly usage **1% used**, Resets **Sep 13**). Native board `4i` stays. Reuse the Cursor in-app WebView session. Do not Custom Tabs. Do not `api2.cursor.sh`. Do not auto-plus `4i` when Cursor signs in.

The live path is still **undocumented and ToS-exposed**. Isolate it behind the Cursor reader. SuperGrok-parented Grok Bot stays **fake** until that surface is researched.

## Sanctioned surfaces (human)

- **Cursor-billed read:** [https://cursor.com/dashboard/spending](https://cursor.com/dashboard/spending) — Grok Bot block, **Weekly usage**, `% used`, `Resets MMM d (N days left)`. Same page as Cursor `4f`.
- Board `4i` seed remains `Included with SuperGrok` for the fake plus when Cursor is not live.

Linking there, or letting them type what they see, is a sanctioned product path. Manual entry (§8.3) stays first-class.

## What we will not do

- open Cursor `1d` from the Grok Bot plus (`usesInAppBrowser(GROK_BOT)` stays false)
- auto-add a Grok Bot tab after Cursor login
- put Grok Bot on Cursor `4f` windows
- expire cursor.com / authenticator cookies when removing Grok Bot
- call DashboardService RPCs or lift cookies into OkHttp
- treat grok.com Usage / SuperGrok weekly as the bot meter
- invent a second Grok Bot window from On-Demand Usage

## The method that ships

Same CookieManager and spending reader as [[Cursor-usage-data-layer]]:

- Plus Grok Bot while a live Cursor tab exists → `addLive` from the last spending parse (or idle 0% weekly, then read-on-open). Without a live Cursor session → still the board fake (61% / SuperGrok parent).
- Header ⟳ and read-on-open on Cursor, or opening an existing fake Grok Bot tab while Cursor is live, re-read spending and `updateLive` `4i`.
- Plan line when Cursor-billed: `Included with Cursor` / `Billed with your Cursor subscription`. Do not keep bolding SuperGrok once that line is stored.
- Window name: board **Weekly session**. Weekly hairline uses `ResetFormatter.SEVEN_DAYS`. Parse `Sep 13` / `Resets Sep 13 (4 days left)`.
- `LiveSignIn.reader(GROK_BOT)` is `CursorWebViewReader`. Conceal only while reading. `NeedsSignIn` on a Grok Bot read marks **Cursor** `1h`, not a Grok Bot-only cookie wipe.
- Remove Grok Bot is `SessionForget.None`.

## Verdict for AI-SUM

| Path | Status |
|---|---|
| Fake `4i` figures | Still used when Cursor is not live, and in tests via `FakeAccountStore.add(grok_bot)` |
| Cursor spending weekly row → native `4i` | **Shipping** when Cursor is signed in. Undocumented. Isolate behind `UsageReader`. |
| SuperGrok-parented live read | Out until researched |
| Official public consumer Usage API | Not available |
| Undocumented DashboardService / cookie lift | Out |

**Auto-Wake is out.** Do not POST as the user.
