# AI-SUM — agent instructions

An Android phone app (Wear OS later) that answers one question per AI subscription: **am I near my limit?**

Every usage window shows two quantities on one bar: **fill** (how much of the allowance is spent) and a **hairline** (how much of the time window has elapsed). Fill ahead of the hairline means the user is burning allowance faster than the clock. That comparison is the product.

> **This file is the single source of truth, for every agent and every tool.** Claude, Cursor,
> Grok, or a human — the rules here are the rules.
>
> It lives in the vault, not at the repo root. The root `AGENTS.md` and `CLAUDE.md` are
> **pointers**. They exist only because Cursor and Claude Code auto-load files by those names.
> **You are not missing anything by not reading them.**
>
> If you learn something every agent needs — a device quirk, a recipe, a trap, a settled
> decision — **write it here**. Session notes and agent-to-agent chatter go in [[Notes]].
> Never put operational knowledge only in a vendor-named file at the repo root.

**Out of scope for v1:** spend tracking, cost, head-to-head comparison, trend charts, dashboards, any server-side account.

---

## Names

| | |
|---|---|
| GitHub | [ericpeck/AI-Subscription-Usage-Monitor-Public](https://github.com/ericpeck/AI-Subscription-Usage-Monitor-Public) |
| Local Android Studio folder | `AISUM` |
| Package | `com.ericmbpeck.ai_sum` |
| **Launcher / app name** | **AI-SUM** (Eric's call, 2026-09-04) |
| Design copy | Still says "Usage Ledger" / screen titles like `Usage`. **Transcribe the board verbatim.** Do not silently rewrite on-screen strings to "AI-SUM". |

The outline and the board call the product Usage Ledger. That is design copy, not the launcher name.

---

## Read before writing any code

This vault is the shared brain. Agent instructions live in `90 Agents/`. Design lives in `10 UI/`. Per-platform evidence lives in `20 Research/`.

Give an assistant **one outline section at a time** when implementing, not the whole file. Sections 3, 5 and 6 are load-bearing.

| File | What it is | Authority |
|---|---|---|
| [[IMPLEMENTATION_OUTLINE]] (`10 UI/`) | Build spec, rev 2, 4 Sep 2026 | **How to build.** Modules, tokens, screens, sequence. **Stale on twice-add** — follow the 4 Sep evening board (thread 5 / 1f–1h) for one account per platform. |
| `10 UI/AI Platform usage tracker/Screens Board.dc.html` | Full board, **25 screens** (adds **1f, 1g, 1h**) | **Design of record** as of 4 Sep 2026 evening. Match the visual output; do not port the DOM. Thread 5 is load-bearing: one live session per platform. |
| `10 UI/AI Platform usage tracker/Screens Board-print.dc.html` | Design rationale and colour table | Read. **Cursor's print-table red is stale** — follow the board (graphite). See outline §14.1. |
| `10 UI/Design.pdf` | 10-page visual (replaced 4 Sep 2026 evening) | Reference. Image-only PDF. |
| `10 UI/AI Platform usage tracker/screenshots/*.png` | Visual acceptance targets, including `p-1f` / `p-1g` / `p-1h` | Present. |
| `10 UI/ai-platform-usage-tracker/project/` | Earlier board (22 screens, twice-add) | History. Not authority. |
| `10 UI/1stDraft-Screens-Board.html` | Earlier snapshot of the board | History. Not authority. |
| `10 UI/AI Platform usage tracker/Usage.dc.html` | Earlier 5-platform prototype | Carries the only working state logic: breach at `usage >= 90`, pace copy. |
| `10 UI/AI Platform usage tracker/_ds/broadsheet-…/styles.css` | Broadsheet tokens | Token source. Ignore `.halftone` through `.cmyk-head` (print treatments). |
| `20 Research/` | One dated evidence note per platform | **What we learned.** `researched` / `updated` in frontmatter. Empty notes are not permission to guess. |
| `AndroidStudioScreenshots/` | Device captures Eric drops in for agents | Local only. Gitignored. **Turnover is expected** — Eric frequently adds and deletes files here. |
| [[Claude]] | Claude teardown (2026-09-04) | Evidence. Rule: [[Claude-usage-data-layer]]. |
| [[Gemini]] | Gemini first pass (2026-09-04) | Evidence. Rule: [[Gemini-usage-data-layer]]. WebView Google login is blocked. |
| [[Codex]] | Codex (pass 2026-09-04; live path 2026-09-09) | Evidence. Rule: [[Codex-usage-data-layer]]. Login `/auth/login`; usage `#settings/Usage`. |
| [[Cursor]] | Cursor (2026-09-04; live path 2026-09-09) | Evidence. Rule: [[Cursor-usage-data-layer]]. Spending URL is `/dashboard/spending`. |
| [[Grok]] | SuperGrok / grok.com (2026-09-04) | Evidence. Rule: [[Grok-usage-data-layer]]. Usage URL confirmed live (X login). Not Grok Bot. |
| [[Grok Bot]] | Grok Bot (2026-09-09) | Evidence. Cursor-billed weekly on spending. Rule: [[Grok-bot-usage-data-layer]]. Not SuperGrok. |
| [[Claude-usage-data-layer]] | Claude verdict + WebView method | Claude live-read. Isolate; no cookie lift. |
| [[Gemini-usage-data-layer]] | Gemini verdict | No consumer Usage API; no WebView login; CCT handoff later. |
| [[Codex-usage-data-layer]] | Codex verdict + WebView method | Codex live-read. Same posture as Cursor. No CLI OAuth. |
| [[Cursor-usage-data-layer]] | Cursor verdict + WebView method | Cursor live-read. Same posture as SuperGrok. No `api2.cursor.sh`. Also parses Grok Bot weekly off spending onto `4i`. |
| [[Grok-usage-data-layer]] | SuperGrok verdict + WebView method | SuperGrok live-read. Same posture as Claude. Not Grok Bot. |
| [[Grok-bot-usage-data-layer]] | Grok Bot verdict + Cursor spending method | Cursor-billed `4i` live-read. SuperGrok-billed stays fake. |
| `usage-ledger-user-initiated-refresh.md` (vault root) | Claude+Gemini refresh sketch | Tracked. **Not** a license to replace native bars with the provider's page. Superseded on UA handling — see Standing rules. |

Where sources disagree, outline §14 records the conflict and names which one to follow.

**What you may edit in this vault:** `90 Agents/` (this file, [[Notes]], and later agent notes) and `20 Research/` (when doing platform research). **What you must not edit unless the task says so:** the outline, the design prototypes, `Design.pdf`, and `.obsidian/`.

---

## Standing rules

Restate these at the start of every implementation session.

- Broadsheet is a **light, square-cornered, serif** system. Radii are 1–2–4 dp. Do not apply Material 3 defaults, do not round corners, do not add a dark theme to the phone app, do not use dynamic colour.
- **Source Serif 4** for everything, headings and body alike. Bundle in `res/font/`; do not fetch at runtime.
- Cyan `#0088B0` / `#006786` is the **only** interactive colour. Red on breach **and** on signed-out / Expired (`1g`, `1h`, `#b3231e`). Gold only on work accounts.
- Usage figures use tabular numerals (`fontFeatureSettings = "tnum"`).
- The Wear screens are **app screens**, not system watch faces. Never use the watch-face APIs.
- `minSdk` is **36** as a temporary prototype floor. Prefer AndroidX wrappers over direct platform APIs so it can be lowered later. Record any direct platform call above API 30 in `COMPAT.md`.
- Do not add dependencies outside the version catalog.
- Do not invent design tokens. If a value is missing, stop and ask.
- Do not modify files outside the module you were asked to change.
- Copy is specified, not suggested. Transcribe board strings into `strings.xml`.
- Plan names and tiers are volatile. Do not hardcode them; seed from the board and keep them in a data source you can update without a release.
- Normalize the in-app WebView UA (`chromeLikeUserAgent` strips `; wv` and `Version/4.0 `) so **Google-as-IdP** completes on third-party sites (claude.ai, cursor.com, later chatgpt.com). Do **not** use UA modification to sign in to Google first-party surfaces (Gemini).

**The usage bar is the product.** Write it by hand, or review it line by line.

---

## Current code (2026-09-04)

Phone slice in `:app`. Wear companion in `:wear` (same `applicationId`, never signs in). Broadsheet is light-only on the phone (no dynamic colour, no dark theme). Watch app screens are black. Source Serif 4 is bundled in `res/font/` on both.

Nav: **1a Empty → 1b Ledger (nothing connected) / 1f Add (connected) / 1g Expired → 1d full-screen in-app WebView (Claude, SuperGrok, Cursor, and Codex) → 4a–4i / 1h signed-out tab**. Gear on 4a toggles **settings mode** (2a/2b) on the same screen; tab switches stay in settings. ⊖ is **Remove** (2c) — that is log-out; there is no separate Sign out. The settings **info** icon opens **2d Information** (own page, no tabs, back to settings). Removing a tab deletes that row. Remove is log-out for **that** platform: Grok drops grok.com / accounts.x.ai / x.com cookies even if Claude stays; Cursor drops cursor.com the same way; Codex drops chatgpt.com / chat.openai.com / auth.openai.com; Claude drops claude.ai. `CookieManager.removeAllCookies` plus WebStorage wipe only when the last **live WebView-session** tab is gone. Google / Apple / GitHub / Microsoft IdP cookies are not cleared when removing Grok, Cursor, or Codex. **Grok Bot plus does not open 1d.** If a live Cursor tab exists, Grok Bot (`4i`) reads the same spending page (Weekly usage → board **Weekly session**); Remove Grok Bot does not expire cursor.com cookies. Without a live Cursor session, Grok Bot still plants the board fake. Perplexity, Copilot, and M365 are not on the add list; their fake board data stays for tests and previews only. Sign-in is a full-screen WebView (same CookieManager as the in-page read). Google / X OAuth popups stay in a child WebView — not Chrome Custom Tabs, which would put the session in Chrome. After a signed-in `claude.ai` URL (`/`, `/new`, `/chat`, … — not `/login`) the app conceals Claude’s own shell with Paper + “Reading usage…”, reads `/usage`, and pops back to 4a. SuperGrok 1d starts at `accounts.x.ai/sign-in?redirect=grok-com` (not grok.com home — that is chat). Auto-read on `?_s=usage` after a session, **or** when grok.com appears right after X / Google / Apple / xAI (post-SSO chat is signed-in; the reader then loads the usage URL). That deep link before login is an Account sheet, not Usage, and a silent failed read must keep the URL latch or 1d overlay-loops. After X SSO, Continue on chat still hydrates the Usage sheet late: wait for `Weekly SuperGrok Limit` / `% used`, prefer that headline or the Voice+Chat+… sum (not the first `%`), parse `Resets September 10, 2026 at 3:01 PM`. Cursor 1d starts at `cursor.com/dashboard` (Google-as-IdP). After login Cursor lands on `/agents` (signed-in home, not usage). Auto-read on `/dashboard/spending`, **or** when cursor.com appears right after Google / GitHub / Apple / authenticator.cursor.sh (post-SSO `/agents` is signed-in; the reader then loads spending). Unauthenticated `/dashboard` and `/agents` without an OAuth previous URL are not auto-read. Codex 1d starts at `chatgpt.com/auth/login` (Google-as-IdP). After login ChatGPT lands on chat (`/`). Auto-read on `#settings/Usage`, **or** when chatgpt.com appears right after Google / Apple / Microsoft / auth.openai.com / `/auth/login` (post-login chat is signed-in; the reader then loads the usage hash). Unauthenticated `/auth/login` is not auto-read. Continue remains the fallback. Conceal only while reading. A failed Continue does not block that auto-read. Spending plan comes from **CURRENT PLAN** (Pro+), not the Upgrade Ultra card. Grok Bot weekly on that page is `4i` only — not a third `4f` window. **Continue to …** remains a header fallback. No cookie lift, no OkHttp, no Auto-Wake. Free disclaimer on 1b and Claude 1d; SuperGrok, Cursor, and Codex 1d have their own free lines, not Claude Free. Free accounts are refused when they have no windows. **0% used is valid SuperGrok.** Claude **Current session** stays on `4a` even when `/usage` omits `five_hour` or `resets_at` (idle window: 0%, `Resets in 5 hr`). Codex **Current session** stays on `4c` when `wham/usage` is weekly-only (idle 5-hour row). Header ⟳ and read-on-open on a live Claude, SuperGrok, Cursor, or Codex tab re-run that platform’s in-WebView read. Opening Grok Bot while Cursor is live re-reads spending onto `4i`. **`NeedsSignIn` marks `needsReauth` and stays on 1h** — it does not open 1d. A Grok Bot `NeedsSignIn` flags **Cursor**, not a Grok Bot cookie wipe. Reauth is **per platform**. The user opens 1d from **Sign in to {Platform}** on 1h, or **Log in again** on 1g (that button only walks to the existing tab). Backing out of 1d leaves 1h — do not auto-loop. A generic read failure does **not** open 1d. Successful re-auth clears `needsReauth` on the live tab of **that** platform. **One live session per platform.** Work is a filing flag at the first plus, not a second tab. A second plus for a connected platform is a no-op; if that row is Expired, it opens the existing tab. Notification toggles persist with the tab list. 2d Share uses the system share sheet with the “What is this app?” paragraph. Donate is on-screen; it does not take payment until Eric picks a rail (outline §14.3 #4). Add-list order: Claude, Codex, Grok, Grok Bot, Cursor. Gemini, Perplexity, Copilot, and M365 are not offered.

In-memory `FakeAccountStore` is **seeded from `filesDir/accounts.json`** at process start (`AiSumApplication`). Cookies stay in `CookieManager`; the JSON is the tab list (platform, plan, last windows, `liveRead`). Empty file / first install still opens 1a. Not Room. The phone pushes that tab list to the watch over DataLayer (`/aisum/snapshot`). The watch never fetches. Empty watch is **3g** (Open on phone via `aisum://open` — `RemoteActivityHelper` requires `ACTION_VIEW` + `CATEGORY_BROWSABLE` + a data URI; MAIN/LAUNCHER is rejected with `IllegalArgumentException`. Phone `WearMessageListenerService` also handles `/aisum/open`). Connected accounts swipe as **3a** / `w-4a–4i`. Tiles **3f** / **3e** and reset notification **3c** are not in this slice. Watch type and spacing copy the board's **454px** `.wv` face through density (`WatchBoardScale`: 76px figure → 38sp on a 454px xxhdpi round, then **1.15×** on centre type only — Eric, 2026-09-10). Curved labels stay on the 380 viewBox. Reset copy (and the Grok legend) sit in **one Y slot** on every face, sized so a wide date still has **16px** of air above the inner weekly curve (`WatchLabelClearance.resetSlotTopFromCenterPx`). Do **not** lift from `positionInRoot` while paging — that used the window centre as the circle and jumped the line into the figure mid-swipe (Eric, `Screen_recording_20260910_120932.webm`). Do **not** paste board CSS pixels as Android `sp`.

| Item | This repo | Outline said |
|---|---|---|
| `minSdk` | 36 | 36 |
| `targetSdk` / `compileSdk` | **37** | 36 |
| Gradle daemon JVM | **25** | JDK 17 |
| Kotlin | 2.2.10 | 2.0+ |
| AGP | 9.4.0 | — |
| Compose BOM | 2026.02.01 | Compose BOM |
| Wear `minSdk` | **34** (Wear OS 5+) | 36 / Wear OS 6 |

Do **not** "fix" `compileSdk` 37 or daemon JVM 25 back to the outline unasked.

GitHub: [ericpeck/AI-Subscription-Usage-Monitor-Public](https://github.com/ericpeck/AI-Subscription-Usage-Monitor-Public). Feature work ships as PRs (see *Workflow*). Never commit to `main`. Eric merges. The original private development history is not imported here.

---

## Build sequence

Phone first. Wear after the phone app works.

| Packages | What | Gate |
|---|---|---|
| 1–10 | Skeleton, Broadsheet, `UsageBar`, domain, nav, phone screens against **fakes** | Phone screens match the board |
| 11–15 | Wear skin, arcs, tiles, DataLayer | Starts only after the phone works |
| **16** | Data-layer findings review | **Nothing from 17 onward until this is read** |
| 17–22 | Manual entry, real providers, notifications, a11y, release | After 16 |

Build packages 1–15 against fakes. **Do not write a real usage parser until package 16**, except the Claude, SuperGrok, Cursor, Cursor-billed Grok Bot, and Codex WebView proofs Eric authorized — still gated on [[Claude-usage-data-layer]], [[Grok-usage-data-layer]], [[Cursor-usage-data-layer]], [[Grok-bot-usage-data-layer]], and [[Codex-usage-data-layer]] (in-WebView read only, no cookie lift, no writes, no CLI OAuth, no `api2.cursor.sh`). Perplexity, Copilot, and M365 wait. Outline §8.1 HTML `parse(page)` is the wrong shape for Claude. Terms of service go to a lawyer, not to an AI assistant.

A window needs only a percentage and a reset time to render. **Manual entry is a first-class citizen** — same bar, hairline, pace copy, watch arc, and reset notification. Downstream must not care where the number came from.

When implementing a screen: stateless composable + `ViewModel`; `UiState` sealed interface with Loading, Empty, Content, Error; the composable takes `uiState` and event lambdas — never the ViewModel. Preview every state. Definition of done is outline §12.

---

## Future module map

Phone stays in `:app`. Wear is `:wear`. Shared snapshot types live in `:core:wearbridge`. Do **not** explode the rest of the outline graph unasked.

```
:app                    // Phone application, NavHost, live readers
:wear                   // Wear OS application (separate APK, same applicationId)
:core:wearbridge        // Snapshot DTO + arc math + DataLayer paths. JVM.
:core:designsystem      // Broadsheet tokens, phone theme — later
:core:designsystem-wear // Wear dark theme, arc primitives — later
```

**Dependency rule:** `:wear` depends on `:core:wearbridge` and Play services Wearable only. It must not pull in WebView, auth, or providers — the watch never signs in. Phone publishes the snapshot; watch renders it.

Add further `:core:*` / `:feature:*` modules when extracting from `:app`, not because Wear needed them.

Nine board screens (`4a`–`4i`). **One tab per platform.** Work is a filing flag at add time, not a second tab. The add list is five platforms — **Gemini, Perplexity, Copilot, and M365 are off** (Eric, 2026-09-09). Grok Bot sits after Grok. Catalog seed is `Included with SuperGrok`; a Cursor-billed live tab stores `Included with Cursor`.

---

## Devices

### Never touch Eric's physical phone

Eric's personal phone is often attached. Never install, launch, or test on it unless he explicitly requests it for the current task. Use a named emulator and resolve its serial dynamically. Standing instruction:

> Don't include my physical phone as a required test. I'll check things there myself and if I
> need you to check things on there, I'll ask you. So don't run anything on my phone unless asked
> specifically by me.

It is never a test target unless Eric names it in that request.

### `AndroidStudioScreenshots/` is ephemeral

`AI-Sub-Use-Monitor-Vault/AndroidStudioScreenshots/` is a drop folder, not an archive. Eric will **frequently delete and add files** there. That is normal.

- Use whatever is in the folder **right now**. Do not treat a missing file as a repo problem, and do not try to restore old captures.
- Never commit that directory (already gitignored). Recordings are huge.
- Design-of-record screenshots stay in `10 UI/AI Platform usage tracker/screenshots/` — those are not this folder.

**Pin `ANDROID_SERIAL` before any Gradle or `adb` command** that talks to a device. Without it, Gradle fans out to every attached device — including the physical phone.

```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
```

Resolve serial by AVD name; emulator ports are dynamic. Strip CR from `adb emu avd name` (`tr -d '\r'`).

Wear hardware `minSdk` is still an open question (outline §14.3 #10). `:wear` is **34** until Eric names a device. Emulator proof is Wear OS 6.1 (`Wear_Round_API_36.1`).

---

## Workflow

- This Grok agent is the **primary implementer**. Claude or another Grok may review or double-check. Treat review findings as binding when Eric agrees; durable rules go **in this file**.
- **Do not ship untested code.** After a feature or a specific fix, **prove the behaviour works before you push or open a PR.** Unit tests that never ran the user path are not enough for product behaviour.
- **After every feature that adds to the app, open a pull request** — only after that proof. Branch off `main`, commit, push the branch, `gh pr create`. Return the PR URL. Eric reviews it, or another agent does, then **Eric merges**. Do not merge. Do not commit to `main`. Do not force-push.
- A "feature that adds to the app" means product behaviour in `:app` (later `:wear` / modules). Include the vault rule updates that document that feature (this file, [[Notes]], the platform data-layer note) in the same PR.
- Do **not** open a PR for vault-only research, gitignore tweaks, or agent notes with no app change, unless Eric asks.
- Never commit secrets (`local.properties`, keystores, cookies, tokens).
- One task at a time. Do not start real providers or the rest of the `:core:*` / `:feature:*` graph because they are in the outline. Wear (`:wear` + `:core:wearbridge`) is in; tiles and 3c are not.
- If a spec is ambiguous, that is a bug in the spec. Stop and ask rather than improvising.
- Be honest about what you did not finish. Session leftovers go in [[Notes]].
- Kotlin official style, 4-space indent. No `!!`, no empty `catch`, no `GlobalScope`, no `runBlocking` outside tests. Strings in `res/values/strings.xml`.
- Commit messages: imperative subject under ~70 chars, then a body explaining *why*.

### Test before you push

Eric, 2026-09-09: we do not ship untested code. **Pushing a PR is the last step, not the first.**

| Change | Required proof |
|---|---|
| Mapper, formatter, store, other pure logic | A unit test that **failed before the fix** and passes after. Then `./gradlew :app:testDebugUnitTest` green. |
| Screens, navigation, persistence, sign-in, live-read, anything the user taps | That **plus** the user path on an **emulator**. Force-stop and reopen when the bug is about later launches. Unit tests alone do not count. |

- Pin `ANDROID_SERIAL` to the emulator. Never use Eric's physical phone unless he names it in that request. See *Devices*.
- If you cannot run the required check (no AVD, needs a live login only Eric can do), **do not push**. Say what is blocked and wait.
- In the PR body, list what you ran: Gradle task, AVD name if any, which screens or actions. Do not write "tested" when you only compiled.

### PR descriptions

Eric, 2026-09-09: PRs need a **detailed** body. A one-line summary is not enough. Use these headings, in this order. Skip **Misc** when there is nothing to say; do not skip the others.

| Heading | What to cover |
|---|---|
| **Purpose** | Why this PR exists. The user-visible problem or goal, not a file list. |
| **Functionality** | What the app does after merge that it did not do before. Behave like a product note. |
| **What was created** | New and changed files, types, screens, vault notes. Name the important ones. |
| **Why it was created this way** | The design choice and the rejected alternatives (for example: in-WebView read vs cookie lift; post-SSO auto-read vs Continue-only). |
| **Testing methodologies** | How proof was gathered: which Gradle task, which AVD, which user path, which unit tests were written to fail first. |
| **Testing results** | What actually happened. Pass/fail, figures on device, recordings or screenshots by name. Do not write "tested" if you only compiled. |
| **Possible concerns or tech debt** | Honest leftovers: unparsed copy, plan-name collisions, paths not exercised, follow-ups. |
| **Misc** | Optional. Links, out of scope, merge notes. |

Cursor agents also see a short pointer in `.cursor/rules/pr-descriptions.mdc`. **This table is the source of truth.**

---

## Open questions

Do not guess at these. Stop and ask.

From outline §14.3:

1. Alert threshold — 90% in `Usage.dc.html`; confirm, and whether it is user-configurable.
2. Quiet hours — implied for work accounts, no screen defines the hours.
3. Tile picker — tiles say the user picks sessions on the phone; that phone screen is not on the board. Blocks package 13.
4. Donate and Share (2d) — which payment rail.
5. Grok Bot parentage — **Cursor-billed path settled 2026-09-09:** weekly usage lives on Cursor spending; native `4i` reuses that session. SuperGrok-billed Grok Bot is still unverified; stay fake for that parent.
6. Work-account detection — manual tick at add time only (`1f` / empty `1b`), or inferred. Not a second tab.
7. Tab overflow — **settled 4 Sep evening:** one tab per platform, not 18. **Gemini, Perplexity, Copilot, and M365 withdrawn 9 Sep:** add list is five (Claude, Codex, Grok, Grok Bot, Cursor).
8. Codex vs ChatGPT — board replaced ChatGPT with Codex; confirm ChatGPT is intentionally absent.
9. Always-on / ambient watch variant — not designed; likely out of v1.
10. Watch hardware Wear OS version — governs `:wear` `minSdk`.

Also unsettled:

- **Grok Bot colour** shares Grok's black. Two adjacent tabs, same hue. Assign a distinct step, or accept the collision knowingly. Outline §14.2.
- **Perplexity, Copilot, and M365 have empty notes in `20 Research/` and are off the add list** (Eric, 2026-09-09), same as Gemini. Fake `4e` / `4g` / `4h` remain for tests and previews. Do not restore unless Eric asks. Codex: [[Codex]] + [[Codex-usage-data-layer]] (live `4c`; login `/auth/login`, usage `#settings/Usage`). Grok Bot: [[Grok Bot]] + [[Grok-bot-usage-data-layer]] (Cursor-billed live `4i`; SuperGrok-billed still fake). Cursor: [[Cursor]] + [[Cursor-usage-data-layer]] (live `4f`). Claude: [[Claude]] + [[Claude-usage-data-layer]]. Gemini: [[Gemini]] + [[Gemini-usage-data-layer]] (researched; **off the add list** 2026-09-09). SuperGrok: [[Grok]] + [[Grok-usage-data-layer]] (live `4d`).
- **Screenshot PNGs are in** `10 UI/AI Platform usage tracker/screenshots/`. `Design.pdf` is `10 UI/Design.pdf` (10 pages, 4 Sep evening). `GrokBotLogo.jpg` is in `10 UI/AI Platform usage tracker/uploads/` — the app still does not use it.
- **Board 4 Sep 18:22:** twice-add leftovers on `1b` / `1e` / `2b` / `2d` are gone. FAQ is **No**. `1e` plus goes to `1f`. `2b` is work **M365**, not a second Claude. **Grok Bot** is on `1b`/`1f` (`Included with SuperGrok`) with its own Work tick and plus. **Gemini was on the add list** (before M365) that night; **Eric took it off 2026-09-09.** **Perplexity, Copilot, and M365** left the add list the same day (core five: Claude, Codex, Grok, Grok Bot, Cursor).

---

## Deliberate divergences

Places where the code (or this repo) knowingly does **not** match the outline. All were Eric's call, or are wizard leftovers we are keeping until told otherwise. **Do not "restore" any of them.** If you find another, add it here in the same change.

| Topic | Outline / board says | This repo does | Why |
|---|---|---|---|
| App name | "Usage Ledger" | Launcher `AI-SUM` | Eric, 2026-09-04. On-screen copy still follows the board. |
| Cursor colour | Print table `#D1201A` | Board graphite `#7D7979` / `#4F4C4C` | Outline §14.1 — follow the board. |
| `targetSdk` / `compileSdk` | 36 | 37 | Android Studio wizard, AGP 9.4. Keep until asked. |
| JDK | 17 | Daemon toolchain 25 | Wizard. Keep until asked. |
| 1d sign-in | Board: bordered in-app pane, lead copy, Continue bar, tab strip | Full-screen in-app WebView, thin chrome, auto-return on session. Perplexity, Copilot, and M365 are off the add list. Grok Bot plus never opens 1d. | Eric, 2026-09-04: Google sign-in stuck in the small pane (Continue bar covered Google's Next; OAuth popups had no window). Custom Tabs would isolate cookies from the in-page `fetch`. See [[Claude-usage-data-layer]], [[Grok-usage-data-layer]], [[Cursor-usage-data-layer]], [[Grok-bot-usage-data-layer]], and [[Codex-usage-data-layer]]. |
| Auto-Wake / Haiku | Competitor POSTs a message at reset | Never. Read only. | Eric, 2026-09-04. ToS slippery slope. |
| 4f extra tabs | Board paints inactive Claude/Gemini/… tabs | Only real account tabs + Add | Do not fake chrome for platforms that are not added. |
| Gemini on 1b / 1f | Board lists Gemini (before M365) | **Not offered.** `addableThisSlice = false`; saved Gemini rows are dropped on load. | Eric, 2026-09-09. Research notes stay. Do not restore the row. |
| Perplexity, Copilot, M365 on 1b / 1f | Board lists all three | **Not offered.** Same `addableThisSlice = false` + drop on load as Gemini. Fake boards stay for tests/previews. | Eric, 2026-09-09. Core five is enough for now. Do not restore unless asked. |
| Wear tiles 3f / 3e | Phone picker chooses which sessions the tiles show | **Not built.** Outline §14.3 #3: that phone screen is not on the board. | Eric, 2026-09-09 Wear slice. App screens 3g/3a first. |
| Wear `minSdk` | Outline 36 / Wear OS 6 | **34** so Wear OS 5 hardware can install. Emulator used Wear OS 6.1. | Open question #10 is still Eric’s call for Wear hardware. |
| Wear APK embedding | Outline `wearApp(project(":wear"))` | Independent Wear APK, same `applicationId`. AGP 9 has no `wearApp` handler. | Sideload both. DataLayer still keys off the shared id. |
| Auto-Wake / Haiku | Competitor POSTs a message at reset | Never. Read only. | Eric, 2026-09-04. ToS slippery slope. |
| 4f extra tabs | Board paints inactive Claude/Gemini/… tabs | Only real account tabs + Add | Do not fake chrome for platforms that are not added. |
| 4d Grok footnote | Outline short form | Board long form (nearly through its week…) | Design of record is the board. |
| 4i Grok Bot mark | 34px `uploads/GrokBotLogo.jpg` | Two-line plan only, no logo | Asset is in `10 UI/…/uploads/GrokBotLogo.jpg`. App still has no logo until that slice. |
| One account per platform | Outline §1 / 6.3 and leftover `1b`/`2d`: add twice, personal + work | **Built.** `1f` replaces `1b` once anything is connected. Connected row = **Active** (no Work tick, no plus). Expired cookie = **Expired** + Log in again → existing tab `1h`, not a new add. `NeedsSignIn` stays on `1h`. Work is a filing flag at first plus only. | Eric + Claude Design, 4 Sep 2026 evening. Cookie jar can hold one session per host. Gemini later withdrawn from the add list (9 Sep). |
| Remove wipes that account’s cookie | Outline 6.7: deletion includes the session cookie | Shared WebView `CookieManager`. Remove expires that platform’s hosts even if another live WebView tab stays. `removeAllCookies` only when no live Claude, SuperGrok, Cursor, **or Codex** tab remains. | Eric, 2026-09-09. Do not clear Google/Apple/GitHub/Microsoft when removing Grok, Cursor, or Codex. |
| 2d Donate | Outline package 10: Donate wired | Button matches the board. Tap does not open a payment rail. | Outline §14.3 #4 — Play Billing vs other rails is still Eric’s call. Do not invent a URL or SKU. |

---

## Style notes for later packages

- Phone: Compose Material 3 **for structure only**. Visual language is Broadsheet, not Material defaults.
- Watch: Wear Compose Material 3 for app screens; tiles are **ProtoLayout**, not Compose. Do not share drawing code between watch home and tiles.
- Storage: Room + DataStore. Async: coroutines + Flow. Background: WorkManager. DI: Hilt. Sign-in: `WebView` + `CookieManager`. Session cookies are credentials — never log them, never put them in crash reports or `adb` output.
- Type-safe Navigation Compose with `@Serializable` routes. No string routes.
- Settings is a **mode** on the platform screen, not a nested graph.
- Breach is not notified in v1. Red on the bar is the only breach signal.
