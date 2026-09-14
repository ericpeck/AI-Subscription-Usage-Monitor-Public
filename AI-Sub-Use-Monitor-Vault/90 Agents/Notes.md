# Agent notes

Scratch pad for session leftovers, review findings that are not yet rules, and agent-to-agent handoff.

Durable rules belong in [[AGENTS]], not here. If a note becomes a standing instruction, move it.

---

## 2026-09-10 — 3g Open on phone did nothing on a Galaxy Watch

Eric: physical Samsung watch, 3g button is a no-op. `RemoteActivityHelper` only sends `ACTION_VIEW` + `CATEGORY_BROWSABLE` + a data URI; the old MAIN/LAUNCHER/`setPackage` intent throws `IllegalArgumentException` (swallowed). Phone now has `aisum://open` on `MainActivity` and a `WearableListenerService` for `/aisum/open` so the tap still works when AISUM is not in the foreground.

## 2026-09-10 — Reset Y jumped while paging (`Screen_recording_20260910_120932.webm`)

Eric: reset time bounces as Wear faces swipe. `clearOfCurveLabel` measured the gap from `positionInRoot` against the window centre, so a mid-swipe page offset lifted *Resets Sun 12:00 AM* into the 1% figure. Reset (and Grok legend) now sit in `WatchLabelClearance.resetSlotTopFromCenterPx` — one slot, inner curve, canonical wide date — not a per-frame lift. The 16px inner-curve air from earlier the same day still holds; only the measurement method changed.

## 2026-09-10 — Reset lifts off the nearest usage-limit curve

Eric: long reset dates crowd the inner weekly label. First pass used a per-frame `WatchLabelClearance` lift. **Superseded the same day** — see the paging bounce note above. Keep the 16px inner-curve buffer; do not bring back `positionInRoot`.

## 2026-09-10 — Wear type 1.25× the strict 454 map

Eric: a few sizes bigger after the density fix. `WatchBoardScale.TYPE_MULTIPLIER` 1.15 on centre type only. Curved labels stay on the 380 viewBox so they do not grow into *Resets in*. 1.25 on both put the reset line back on the weekly curve.

## 2026-09-10 — Wear centre stack was 2× too big (`Screen_recording_20260910_113430.webm`)

Live 4a/4c/4d/4f/4i on the paired Wear AVD: 76sp figure, name on the arc, *Resets in* through the weekly curve, Grok Voice/Chat/Coding colliding with the outer label, Grok Bot mark in the ring. Board `.wv` is 454 CSS px; those sizes were copied as Android `sp` on a density-2 454px round (76sp = 152px). `WatchBoardScale` maps board px through density. Grok 4d legend is the 118px three-row swatch list from the board, not SpaceBetween rows.

## 2026-09-09 — Wear OS companion (3g / 3a)

Eric: phone works; add Wear. `:wear` + `:core:wearbridge`. Watch never signs in. DataLayer `/aisum/snapshot` from the phone tab list. Empty is 3g; swipe faces are 3a. Tiles and 3c left for later (tile picker is not on the board). `minSdk` 34. Debug extra `--ez sample true` paints board Claude on a unpaired emulator.

---

## 2026-09-09 — Codex live 4c (ChatGPT WebView)

Eric: start 1d at `chatgpt.com/auth/login`; usage at `chatgpt.com/#settings/Usage`. Same in-app WebView as Claude/Cursor. Rule: [[Codex-usage-data-layer]]. Plus Codex now opens 1d.

---

## 2026-09-09 — Codex research promoted; still fake

`20 Research/Codex.md` was an empty stub while `Claude outputs/codex-handoff-for-grok.md` claimed it was researched. Vault now has the evidence (pass 4 Sep, claims corrected 9 Sep). No `Codex-usage-data-layer` — stay fake on `4c`. Open question #8 still Eric’s: `wham/usage` is Codex/agentic, not ChatGPT chat.

---

## 2026-09-09 — 4a / 1f plan copy vs the board

Board `4a` and `1f` use **Max 5×** as sample Claude copy. Observed during private testing: a live Claude plan can be **Pro** while the board still shows Max 5×. Live read was storing `Signed in · rolling session and weekly caps` because `subscription_details` has no plan label, so 4a and the 1f Active line both said Signed in. Plan now comes from the org `rate_limit_tier` (Pro / Max 5× / Max 20×). Fake board seed is unchanged for tests.

---

## 2026-09-09 — Perplexity, Copilot, and M365 off the add list

Eric: hide those three; core five (Claude, Codex, Grok, Grok Bot, Cursor) is enough for now. Same as Gemini: `addableThisSlice = false`; 1b/1f skip them; `accounts.json` drops saved rows on load. Fake `4e` / `4g` / `4h` remain for tests/previews. Do not restore unless Eric asks.

---

## 2026-09-09 — Gemini off the add list

Eric: remove Gemini from the list of AI services. `addableThisSlice` is false; 1b/1f skip it; `accounts.json` drops a saved Gemini row on load. Fake `4b` remains for tests/previews only. Do not restore unless Eric asks.

---

## 2026-09-09 — Grok Bot 4i was board-fake 61% while spending showed 1%

`Exibit1.png` (`cursor.com/dashboard/spending`): Grok Bot **Weekly usage 1% used**, **Resets Sep 13 (4 days left)**. Native `4i` still planted the board (61% / Window 86% / Included with SuperGrok) because Grok Bot was never a live reader. Cursor-billed `4i` now reuses the Cursor spending read. SuperGrok-parented Grok Bot stays fake. Do not auto-plus `4i` when Cursor signs in. Remove Grok Bot does not drop Cursor cookies.

---

## 2026-09-09 — 4f said Ultra on a Pro+ account

Spending (`Screen_recording_20260909_154951.webm`) shows CURRENT PLAN **Pro+ $60/mo** and an Upgrade **Ultra** card. The DOM reader took the first `Ultra`. Percents 27 / 12 matched. Plan now prefers CURRENT PLAN.

---

## 2026-09-09 — Cursor live 4f ready to PR

Eric: “Looks good.” Native 4f after Google (`Screen_recording_20260909_154951.webm`: picker → spending → Cursor Models 27% / Other Models 12%). Grok Bot 4i “Included with SuperGrok” space confirmed on emulator. Opening `feature/cursor-live-4f`.

---

## 2026-09-09 — after Google login, 1d stayed on cursor.com Agents (`Screen_recording_20260909_144529.webm`)

Same class as grok.com chat after X (`Screen_recording_20260909_131212.webm`). Google → cursor.com Agents with Continue still showing; Eric had to open Settings → Spending, then the read ran (native 4f: Ultra, Cursor Models 27%, Other Models 12%). Auto-read on spending still worked once that URL loaded. Post-SSO cursor.com after Google / GitHub / Apple / authenticator now auto-reads the same path Continue does. Continue stays as fallback. Unauthenticated `/dashboard` / `/agents` without an OAuth previous URL still do not auto-read.

---

## 2026-09-09 — Cursor live 4f (Grok-shaped)

Eric: 1d starts at `cursor.com/dashboard` (Google sign-in). After login Cursor lands on `/agents`. Continue / ⟳ load `cursor.com/dashboard/spending` onto native 4f. Post-SSO `/agents` auto-reads (see note above). No `api2.cursor.sh`. Remove Cursor does not wipe Google cookies. Unit tests: `./gradlew :app:testDebugUnitTest` green. Emulator `Medium_Phone_API_36.1`. Eric confirmed Google → native 4f and the Grok Bot space.

---

## 2026-09-09 — after X login, 1d stayed on grok.com chat (`Screen_recording_20260909_131212.webm`)

Cookie wipe worked (fresh “Log into your account”). X → Google picker → 2FA → `accounts.x.ai` completing sign-in → grok.com “What should we explore?” with Continue still showing. Auto-read only fired on `/?_s=usage`, so the app did not leave 1d until Continue. Post-SSO grok.com after an OAuth host now auto-reads the same path Continue does. Continue stays as fallback.

---

## 2026-09-09 — Grok cookies survived Remove while Claude stayed (`Screen_recording_20260909_125947.webm`)

Claude was Active; Grok was already off the tab bar. Add Grok opened `accounts.x.ai` → **Redirecting** → grok.com chat already signed in (stale session). Remove is log-out per platform: expire Grok/X cookies and WebStorage even if Claude remains, and again before 1d loads the sign-in URL (so leftover cookies from older builds cannot skip login). Do not wipe Google/Apple when dropping Grok. Native 4d in this recording already showed 5% / Voice 3 / Chat 2 / Resets Thu 3:01 PM — that parse fix held.

---

## 2026-09-09 — SuperGrok after X login (recording `Screen_recording_20260909_124339.webm`)

After X SSO, 1d is grok.com chat (“What should we explore?”) until Continue. Continue loads `/?_s=usage`; the Usage sheet hydrates late. We were reading too soon (`Could not read usage` while Weekly SuperGrok Limit was on screen), then taking the first `%` (Voice 3%) instead of **5% used** (Voice 3 + Chat 2), and dropping `Resets September 10, 2026 at 3:01 PM` because the mapper only parsed ISO. Wait for that sheet copy, keep going past a stray 401, prefer `% used` / product sum, parse the English reset. Grok stays weekly-only. Live X Continue still needs Eric on device before push.

---

## 2026-09-09 — test before you push

Standing rule (Eric): do not ship untested code. Product behaviour needs the user path on an emulator before `git push` / `gh pr create`. Unit tests alone are not enough for screens, nav, persistence, sign-in. See [[AGENTS]] *Workflow*.

---

## 2026-09-09 — persist tabs; Claude 4a always has Current session

Cold start was sending signed-in users to 1a because `FakeAccountStore` died with the process while WebView cookies survived. Tabs now restore from `filesDir/accounts.json`. Claude live-read no longer drops the 5-hour row when `five_hour` is missing or has no `resets_at`.

---

## 2026-09-04 evening — one account per platform (`1f` / `1g` / `1h`)

Shipped on `feature/one-account-per-platform`. Gemini is back on the add list, second from last (before M365). Connected add list is **1f** (Active, no plus). Dead cookie is **1g** (Expired + Log in again → existing tab) and **1h** (signed-out tab, red Sign in, dimmed last figures). `NeedsSignIn` stays on 1h; it does not auto-open 1d.

---

## 2026-09-04 — stop here (Eric going to bed)

### Built today (phone fakes)

- 1a → 1b → fake **4a–4i** for all nine. Add on the tab strip always returns to 1b. 1d not in code yet.
- Grok 4d: segmented bar Voice 19 / Chat 5 / Coding 20, 1dp gaps, longer board footnote.
- Grok Bot 4i: two-line SuperGrok plan, no logo in the app. `GrokBotLogo.jpg` is in `10 UI/…/uploads/`. Same black inks as Grok.
- Header icons: Phosphor Duotone **2.1.1** `arrows-clockwise` / `gear-six`, 21dp in a 44dp target, 8dp gap, no Material ripple. Earlier vectors were incomplete (gear was a faint ring).

### Vault layout (2026-09-04)

- `10 UI/` — board, outline, Design.pdf, prototype. Do not edit unless asked.
- `20 Research/` — one dated note per platform. Claude, Gemini, Cursor, SuperGrok, Grok Bot, and Codex have passes; Perplexity / Copilot / M365 notes may still be empty stubs.
- `90 Agents/` — rules.

Design of record (4 Sep 18:22): `10 UI/AI Platform usage tracker/` + 10-page `Design.pdf`. Thread 5 = **1f Active / 1g Expired / 1h signed-out**. Twice-add copy cleaned (FAQ No; 1e plus → 1f; 2b = work M365). Grok Bot back on the add list. Gemini was missing from the mockup list; Eric put it back towards the bottom. Outline still says twice-add — ignore that. App now has 1f/1g/1h (see evening note above).

### Claude live-read

Shipped this slice: 1d WebView, in-page `/usage`, Free disclaimer on 1b+1d, no Auto-Wake. SuperGrok is live too; the other seven stay fake.

**Login (2026-09-04 evening):** the board's small bordered pane stuck after Google sign-in — the orange Continue bar sat on top of Google's Next, and OAuth popups had no window. 1d is now a full-screen in-app WebView (same CookieManager). Do **not** "fix" this with Chrome Custom Tabs. Auto-return to 4a when the URL looks signed-in (`/` included — that is Claude’s signed-in home, not the marketing page); Continue stays as a header fallback.

**Login (2026-09-04, after Google):** recording `Screen_recording_20260904_153807.webm` — signed-in Claude home (personalized greeting) stayed on 1d with “Finish signing in… then continue” until Continue was tapped, then Claude’s UI ghosted over 4a. Cause: auto-read skipped `/`; a failed Continue set `Message` and blocked later auto-read; WebView stayed visible during the fetch. Fix: auto-read `/`, clear that latch, read the top Claude WebView, Paper overlay while reading.

Evidence: [[Claude]]. Rule: [[Claude-usage-data-layer]].

### SuperGrok live-read

Shipped this slice: 1d WebView, usage read from `/?_s=usage` onto native `4d`. **2026-09-04 recording `Screen_recording_20260904_163858.webm`:** starting 1d on `/?_s=usage` auto-read immediately, hid login, showed grok.com Account (Language / Birth Year), overlay-looped. **2026-09-04 recording `Screen_recording_20260904_164934.webm`:** grok.com home is chat (“What should we explore?”) + cookie banner + Sign in in the site chrome — not the login form; Eric had to hunt Sign in, then Google → X authorize. 1d start is now `https://accounts.x.ai/sign-in?redirect=grok-com&…`. Auto-read only with `?_s=usage`.

**2026-09-04 recording `Screen_recording_20260904_170122.webm` (~3:51, PR #7 start URL):** xAI host is correct, but the page is blank white ~10s every open. First add reused leftover grok.com cookies, Continue read 1% Chat, then Remove wiped CookieManager. Second add: Login with X → x.com → Continue with Google → Google **“Couldn't sign you in / This browser or app may not be secure”** (twice) plus 2SV to a physical phone; recents-killed the app. Third add: same X chain plus authenticator 2FA plus xAI SSO consent plus “Verifying your device”; returned to grok.com chat + cookie banner + “Open Grok”; Continue then native `4d` 1% Chat. Do not treat this as “start URL still wrong.” Next pain: Google embedded-browser block, cookie banners, Continue required after SSO (no auto-read on grok.com home).

### Cursor

[[Cursor-usage-data-layer]] unchanged: no public Usage API; do not call `api2.cursor.sh` RPCs.

### User-initiated refresh sketch (vault root)

`usage-ledger-user-initiated-refresh.md` — same-night architecture note. Keep: no background poll, Claude WebView, Gemini → Custom Tabs not WebView. **Do not ship Tier A** (provider page as the screen); the board is native bars. Claude live-read is already their Tier B. Gemini research pulled into [[Gemini]] + [[Gemini-usage-data-layer]]. Claude 4a now reads on open as well as ⟳.

**2026-09-04 (Claude review, vault-only):** AGENTS authority table cites this **tracked** vault-root copy, not the gitignored `20 Research/Notes from Grok Bot/` drop. Byte-identical duplicate in that inbox was deleted. Sketch UA lines marked **superseded**; standing rule is now in [[AGENTS]] (normalize UA for Google-as-IdP on third-party sites; never to sign in to Gemini). Claude's verify grep for `UA spoofing` only hits the table row — the "What this is *not*" bullet now says "UA modification".

### Settings 2a–2c

Shipped on `feature/settings-2a-2c` (includes the full-screen login cherry-pick that missed PR #1’s merge). Gear = settings mode; ⊖ = Remove (log-out); 2c confirm. Cookie wipe only if that was the last live Claude **or SuperGrok**. `NeedsSignIn` → 1d, work flag preserved. Notify toggle is in-memory; quiet hours remain unexplained on a screen (outline §14.3 #2).

### Information 2d

Shipped on `feature/info-2d`. Settings info icon opens the Q&A page; back returns to settings. Share uses the system sheet. Donate is painted; payment rail still open (outline §14.3 #4).

### Git / devices

- Eric, 2026-09-04: after every **app** feature, open a PR. He or another agent reviews; **he merges**. Rule is in [[AGENTS]] *Workflow*.
- Never install, launch, or test on Eric's physical phone unless he explicitly requests it for the current task. Use a named emulator and resolve its serial dynamically.
