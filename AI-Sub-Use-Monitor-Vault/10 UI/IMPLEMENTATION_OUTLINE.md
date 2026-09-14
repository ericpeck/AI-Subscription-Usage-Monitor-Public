# Usage Ledger — Android + Wear OS Implementation Outline

**Revision 2** · 4 September 2026
**Source:** Claude Design handoff bundle `AI_Platform_usage_tracker-handoff.zip`
**Design of record:** `project/Screens Board.dc.html` — the full board, 22 screens
**Design review record:** `project/Screens Board-print.dc.html`
**Version shown in mockups:** 0.4.2 beta (build 118), 30 Aug 2026

### Changes since revision 1

| Section | Change |
|---|---|
| 2.0 | **New.** Android Studio bootstrap — template choice and what to delete first |
| 2.1 | `minSdk` raised from 26 to **36** as a deliberate prototype floor |
| 2.3 | **New.** How to build now so the later downgrade is cheap |
| 7.0 | **New.** Wear version mapping, and a warning about the word "face" |
| 8.0 | Data-layer viability is now under investigation; see `GROK_BOT_BRIEF-data-layer.md` |
| 14.2 | Grok Bot confirmed as a real, distinct xAI product — colour question sharpened |
| 14.3 | Open question 5 partly answered by the SpaceX–Cursor merger |

---

## 0. How to use this document

Every value here is transcribed from the design source. Where sources disagree, Section 14 records the conflict and names which one to follow.

Give an AI assistant **one section at a time**, not the whole file. Sections 3, 5 and 6 are load-bearing: get them right and the rest follows.

### Files in the bundle and what to do with them

| File | Status |
|---|---|
| `Screens Board.dc.html` | **Read.** Complete markup for all 22 screens. Source of truth. |
| `Screens Board-print.dc.html` | **Read.** Design rationale and the colour table. |
| `Usage.dc.html` | **Read.** Earlier 5-platform prototype. Carries the only working state logic — breach threshold, pace copy. |
| `_ds/broadsheet-.../styles.css` | **Read.** Token source. Ignore everything from `.halftone` through `.cmyk-head` — CMYK print treatments for the review document, not the app. |
| `_ds/broadsheet-.../_ds_bundle.js` | **Discard.** `"components":[]` — contains only SVG print-plate filters. No app components. |
| `doc-page.js`, `deck-stage.js`, `support.js` | **Discard.** Claude Design prototype runtime — paged-document shell, deck stage, template engine. Zero app logic. |
| `android-frame.jsx` | **Discard.** Device bezel for the prototype canvas. |
| `screenshots/*.png` | **Reference.** Visual acceptance targets. |
| `uploads/device_bezel.png`, `device_mask.png`, `hardware.ini`, `layout` | **Use.** Wear OS emulator skin — Section 7.1. |
| `uploads/GrokBotLogo.jpg` | **Use.** Grok Bot tab and centre mark. |

Do not port the prototype's DOM structure. Compose is a different layout model and a direct port fights it. Match the *visual output*.

---

## 1. Product definition

One screen per subscription, answering one question: **am I near my limit?**

Every usage window shows two quantities on one bar:

- **Fill** — how much of the allowance is spent.
- **Hairline** — how much of the *time window* has elapsed.

Fill ahead of the hairline means the user is burning allowance faster than the clock. That single comparison is the product.

**Out of scope for v1:** spend tracking, cost, head-to-head comparison, trend charts, dashboards, any server-side account.

### Platforms (9)

| Tab | Full name | Windows | Note |
|---|---|---|---|
| Claude | Claude | 2 | Rolling session + weekly |
| Gemini | Gemini | 2 | Pro session + Pro weekly |
| Codex | Codex | 2 | Rides the ChatGPT plan |
| Grok | Grok | **1, segmented** | One weekly limit split by use |
| Perplexity | Perplexity | 2 | Pro searches + Labs |
| Cursor | Cursor | 2 | Cursor Models + Other Models |
| Copilot | GitHub Copilot | 2 | Premium requests + Chat |
| M365 | Microsoft 365 Copilot | 2 | Chat + Agent credits |
| Grok Bot | Grok Bot | **1** | Rides SuperGrok *or* Cursor |

Any platform can be added twice — once personal, once work. Each instance gets its own tab.

---

## 2. Bootstrap, toolchain, module structure

### 2.0 Creating the project in Android Studio

**Template: Empty Activity** under *Phone and large screens*. It is the Compose template.

Do not use anything with "Views" in the name — those generate XML layouts and Fragments, and this app is Compose throughout. Do not use "No Activity" — you would hand-write the manifest and launcher for no benefit. Gemini API, Game Activity, Native C++ and Kotlin Multiplatform are all irrelevant; the watch app is Android too, so KMP buys nothing.

Wizard settings:

- **Minimum SDK:** API 36 — see 2.1
- **Build configuration language:** Kotlin DSL
- **Package name:** decide the final one now. The Wear module must share it.

**Immediately after it generates, delete two things.** The template writes `ui/theme/Color.kt`, `Theme.kt` and `Type.kt` containing a Material 3 **dynamic-color scheme** and a **dark theme**. Both contradict Broadsheet, which is light-only with fixed platform inks. Leave the dynamic-color branch in and Android will recolour your platform hues from the user's wallpaper — every colour in the design becomes wrong. Remove both before writing anything else.

**Keep `enableEdgeToEdge()`.** You need it for the pinned tab strip and inset handling.

Two things the wizard will not do:

1. **The modules in 2.2.** The wizard only ever creates `:app`. Add the rest via *File → New → New Module → Android Library*.
2. **The watch app.** Do **not** start a second project from the Wear OS template. Once this project exists, use *File → New → New Module → Wear OS* to add `:wear` inside it, so both APKs share the package and the DataLayer bridge works.

### 2.1 SDK levels

| Module | `minSdk` | `targetSdk` | `compileSdk` |
|---|---|---|---|
| `:app` | **36** | 36 | 36 |
| `:wear` | **36** *(see warning)* | 36 | 36 |
| `:core:*` shared | 36 | — | 36 |

**API 36 is a deliberate prototype floor, not a shipping decision.** You are the only user, so reach is irrelevant, and at 36 there are no compat branches: every API is available unconditionally and there is exactly one behaviour to test against. Revisit once the app works — Section 2.3 explains how to make that cheap.

> **Warning — check your watch hardware first.** API 36 on the wrist means **Wear OS 6**. If your actual device runs Wear OS 5.1 (API 35) or earlier — a Pixel Watch 3, for instance — a `:wear` module at `minSdk 36` will not install on it, and you will discover that at package 11 with the arc geometry already written. Set `:wear` to whatever your real device runs, or accept emulator-only for the watch. **The two modules do not have to match.**

Reach, for when you revisit: cumulative install base sits near 79.8% at API 30, 87.1% at 29, 91.7% at 28 and 95.7% at 26.

**Play policy, which governs `targetSdk`, not `minSdk`:**

- From 31 August 2026, new apps and updates must target API 36 or higher — Wear OS and Automotive excepted.
- Wear OS apps must target API 34 or higher.
- From 15 September 2026, all Wear OS apps must support 64-bit devices. A non-issue for pure Kotlin, but it is imminent.

### 2.2 Rest of the toolchain

| Item | Value |
|---|---|
| JDK | 17 |
| Kotlin | 2.0+ (K2) |
| Compose | Compose BOM — never pin artifacts individually |
| Phone UI | Compose Material 3 |
| Watch UI | Wear Compose Material 3 |
| Tiles | **ProtoLayout** (`androidx.wear.protolayout`) — not Compose |
| Storage | Room + DataStore (Preferences) |
| Async | Coroutines + Flow |
| Background | WorkManager |
| Phone↔Watch | `Wearable.getDataClient()` |
| Sign-in | `android.webkit.WebView` + `CookieManager` |
| DI | Hilt |

### Modules

```
:app                    // Phone application, NavHost, DI graph
:wear                   // Wear OS application (separate APK, same package)
:core:designsystem      // Broadsheet tokens, phone theme        ← §3
:core:designsystem-wear // Wear dark theme, arc primitives       ← §7.2
:core:ui                // UsageBar, WindowRow, TabStrip, header
:core:model             // Pure Kotlin: Platform, Account, UsageWindow
:core:data              // Repositories, provider registry
:core:database          // Room entities, DAOs
:core:auth              // WebView session capture, encrypted cookie store
:core:providers         // One reader per platform (9)           ← §8
:core:sync              // WorkManager jobs, DataLayer bridge
:core:testing           // Fakes, Compose test rules
:feature:onboarding     // 1a, 1b, 1d, 1e
:feature:platform       // 4a–4i
:feature:settings       // 2a, 2b, 2c
:feature:info           // 2d
```

**Dependency rule:** `:feature:*` → `:core:*` only. No feature depends on another feature. `:wear` shares `:core:model`, `:core:database`, `:core:sync` and nothing else — it must not pull in `:core:auth` or `:core:providers`, because the watch never signs in (see 7.7).

A library module's `minSdk` must be at or below that of every app consuming it. When the floors diverge, set shared `:core:*` modules to the **lower** of the two.

Enforce the dependency rule with a Gradle check. AI assistants breach module boundaries silently.

### 2.3 Designing now for the eventual downgrade

Lint's `NewApi` check catches API-surface violations mechanically the moment you lower the floor. It cannot catch **behavioural** differences — and your riskiest subsystem is exactly the kind that shifts silently across versions. WebView cookie semantics, `SameSite` defaults, background execution limits and notification delivery all changed between API 29 and 36 without changing a single method signature. A session-capture flow validated only on Android 16 tells you nothing about Android 10.

Five practices that turn the downgrade into an afternoon rather than a rewrite:

1. **One constant, one edit.** Put `minSdk` in the version catalog or a `build-logic` convention plugin. Never repeat the literal across fifteen modules.
2. **Turn `NewApi` on as an error today.** It stays silent at 36 and starts working the instant you lower.
3. **Prefer AndroidX over platform APIs wherever both exist.** `enableEdgeToEdge()` over raw `WindowInsetsController`; `androidx.core.splashscreen` over the platform splash API. Room, DataStore and WorkManager are already compat-layered. Lean on AndroidX throughout and most of the downgrade is free.
4. **Log every direct platform-API call above API 30** in a `COMPAT.md` as you write it. That file is your downgrade worklist.
5. **Keep the WebView layer behind its own interface.** When you lower the floor, you re-test `:core:auth` against an old emulator image and nothing else.

One thing API 36 gets right by default: `POST_NOTIFICATIONS` is a runtime permission from the start, so package 18 is built correctly rather than retrofitted.

---

## 3. Design system — Broadsheet

The app's visual language is a **light newspaper**: paper ground, serif throughout, hairline rules, thick-thin rule pairs as furniture. There is no dark theme on the phone — the design system has no dark surfaces.

### 3.1 Base tokens (from `styles.css`)

```kotlin
// core/designsystem/Color.kt
val Paper   = Color(0xFFF3F2F2)  // --color-bg
val Surface = Color(0xFFEAE9E9)  // --color-surface
val Ink     = Color(0xFF201E1D)  // --color-text
val Accent  = Color(0xFF0088B0)  // --color-accent — cyan, the ONLY interactive colour
val Divider = Ink.copy(alpha = 0.16f)

val Neutral100 = Color(0xFFF8F4F4); val Neutral200 = Color(0xFFEAE7E7)
val Neutral300 = Color(0xFFD7D3D3); val Neutral400 = Color(0xFFBAB6B6)
val Neutral500 = Color(0xFF9B9797); val Neutral600 = Color(0xFF7D7979)
val Neutral700 = Color(0xFF605D5D); val Neutral800 = Color(0xFF444141)
val Neutral900 = Color(0xFF2D2B2B)

val Accent600 = Color(0xFF1186AC); val Accent700 = Color(0xFF006786)
val Accent800 = Color(0xFF004961)
```

Spacing: `5 / 10 / 15 / 20 / 30 / 40` dp. Radii: `1 / 2 / 4` dp — **near-square by design**. Do not round corners to Material defaults.

Elevation: `shadow-sm = 0 1dp 2dp, ink @ 14%`. Used sparingly.

### 3.2 Typography

**Source Serif 4** for everything — headings *and* body. Weights 400 and 600. Bundle in `res/font/`; do not fetch at runtime.

| Role | Size | Weight | Tracking | Colour |
|---|---|---|---|---|
| Screen title | 30sp | 600 | −0.025em | Ink |
| Title suffix ("settings") | 15sp | 400 | 0 | Neutral600 |
| Screen meta (uppercase) | 11sp | 400 | +0.1em | Neutral600 |
| Plan line | 13sp | 400 | 0 | Neutral700 |
| Window name | 19sp | 600 | 0 | Ink |
| **Usage figure** | 30sp | 600 | −0.02em | platform *figure* hue |
| Reset / elapsed labels | 12sp | 400 | 0 | Neutral700 |
| Footnote | 12sp | 400 | lh 1.6 | Neutral600 |
| Section head (uppercase) | 13sp | 600 | +0.08em | Neutral700 |
| Tab label | 13sp | 600 | −0.01em | active: figure hue · inactive: Neutral600 |
| Work mark "W" | 11sp | 700 | +0.04em | `#8A6A12` |
| Add / Remove label | 10sp | 600 | +0.04em | Accent700 · `#A81C13` |
| Info page H3 | 21sp | 600 | −0.015em | Ink |
| Empty-state headline | 27sp | 600 | −0.02em | Ink |

All usage figures use `fontFeatureSettings = "tnum"` so they do not jitter on refresh.

### 3.3 Platform inks — phone

`bar` paints the fill; `figure` is a deeper step so the display number clears 3:1 on paper.

| Platform | Bar | Figure |
|---|---|---|
| Claude | `#D97757` | `#B8552F` |
| Gemini | `#4285F4` | `#2F66C9` |
| Codex | `#10A37F` | `#0B7D61` |
| Grok | `#201E1D` | `#201E1D` |
| Perplexity | `#20808D` | `#196A75` |
| **Cursor** | `#7D7979` | `#4F4C4C` — graphite, see §14.1 |
| GitHub Copilot | `#6E40C9` | `#5A2FA8` |
| Microsoft 365 | `#0F6CBD` | `#0B5699` |
| Grok Bot | `#201E1D` | `#201E1D` — see §14.2 |

Grok's split-by-use segments, darkest first: `#201E1D` · `#56524F` · `#8C8783`.

### 3.4 Semantic colours

| Role | Value | Used only for |
|---|---|---|
| Breach bar | `#D92B1F` | A window at or over the alert threshold |
| Breach figure | `#A81C13` | The figure on a breached window |
| Destructive action | `#B3231E` | The Remove button in the confirm dialog |
| Work mark | `#8A6A12` | Gold **W** on work accounts (phone) |
| Interactive | `#0088B0` / `#006786` | Every tappable affordance |

**Rule:** red never appears except on breach. Gold never appears except on work accounts. Cyan is the only interactive colour. Enforce in review.

---

## 4. Component library — build before any screen

Build in `:core:ui`, in isolation, with previews for every state.

### 4.1 `UsageBar` — the core primitive

The single most important component in the app. Everything else is chrome around it.

```
Track:     10dp tall, full width, Neutral200, square corners
Fill:      0..usage% of width, platform bar hue, left-aligned, square
Hairline:  1dp wide, Ink, at elapsed% of width,
           extending 5dp ABOVE and 5dp BELOW the track
Top margin: 12dp from the row above
```

```kotlin
@Composable
fun UsageBar(
    usageFraction: Float,             // 0f..1f
    elapsedFraction: Float,           // 0f..1f
    barColor: Color,
    segments: List<Segment>? = null,  // non-null → split by use
    modifier: Modifier = Modifier,
)
data class Segment(val label: String, val fraction: Float, val color: Color)
```

**Segmented variant (Grok, 4d):** the fill area subdivides into segments laid out proportionally with a 1dp gap between each. Segment fractions sum to the total usage fraction. The hairline still sits at elapsed%.

**Animation:** width animates over 350 ms on refresh. Nothing else animates.

Previews required: 0%, 45%, 94% breached, fill behind hairline, fill ahead of hairline, three-segment.

### 4.2 `WindowRow`

```
Row 1:  [window name 19sp] ...... [figure 30sp, right, figure hue]   baseline-aligned
Row 2:  UsageBar                                                     12dp top margin
Row 3:  [reset 12sp] ...... [elapsed 12sp]   space-between,          7dp top padding
Bottom padding: 26dp
```

For segmented windows a wrapping legend follows Row 3 at 14dp top padding: 8dp square swatch + label 13sp + value 13sp/600, gaps `7dp × 18dp`.

Breach: when `usage >= alertThreshold`, `barColor` → `#D92B1F` and the figure → `#A81C13`.

### 4.3 `PlatformTabStrip`

Pinned to the bottom of every platform and settings screen.

```
Thin rule:   1dp, Ink, full width
Thick rule:  3dp, Ink, full width, 2dp below the thin rule
Row:
  ├─ Scrollable tab list (weight 1f, clipped, scrollbar hidden)
  │    Tab: min 74dp wide, min 56dp tall, padding 9/8/7dp
  │         [optional gold W 11sp] [label 13sp/600]
  │         indicator 2dp × 18dp, 5dp gap below the label
  │         active → platform bar hue; inactive → transparent
  │    Scroll-position hairline below the list:
  │         two 2dp bars, padding 0/8dp/2dp, gap 2dp
  │         thumb Neutral500, track Neutral200, proportional flex
  ├─ Vertical divider: 1dp, Divider, margins 6dp/4dp/8dp
  └─ Fixed action button: 54dp wide, min 56dp tall
       normal mode:   ⊕ 21sp, "Add",    Accent700
       settings mode: ⊖ 21sp, "Remove", #A81C13
```

The action button is **fixed** — it never scrolls with the tabs.

### 4.4 `ScreenHeader`

```
Padding 14dp / 20dp / 0
Row:  [title 30sp, weight 1f]  [icon 44×44dp]*
Meta: 11sp uppercase Neutral600, 2dp top / 8dp bottom
Rule: 1dp Divider
```

| Screen | Icons |
|---|---|
| Platform (4a–4i, 1e) | ⟳ refresh (figure hue) · ⚙ gear (Neutral600) |
| Settings (2a–2c) | ⓘ info (Neutral600) · ⚙ gear (figure hue — lit to show mode) |
| Info (2d) | ← back |

Icons are **Phosphor Duotone**. Bundle the needed subset as vector drawables; do not add a web-font dependency. Used: `arrows-clockwise`, `gear-six`, `info`, `plus-circle`, `minus-circle`, `caret-down`, `caret-left`, `lock-simple`, `device-mobile`, `cloud-slash`, `arrow-counter-clockwise`.

### 4.5 Others

- `PlatformSwatch` — 10dp square (8dp in tiles, 11dp on the watch centre), platform bar hue. **Square, never a circle.**
- `WorkMark` — gold **W**, 11sp/700, `+0.04em`, sits *before* the platform name.
- `Toggle` — 46 × 24dp track, radius 12dp; 18dp knob at 3dp inset (off) or 25dp (on); knob Paper with `shadow-sm`. Track = platform bar hue when on, Neutral300 when off. Tap target 52 × 44dp.
- `SectionRule` — 1dp Divider.

---

## 5. Domain model

```kotlin
enum class AccountKind { PERSONAL, WORK }

data class Platform(
    val id: String,            // claude, gemini, codex, grok, perplexity,
                               // cursor, copilot, m365, grokbot
    val displayName: String,   // "Microsoft 365 Copilot"
    val tabLabel: String,      // "M365"
    val barColor: Long,
    val figureColor: Long,
    val watchOuterColor: Long,
    val watchInnerColor: Long,
    val availablePlans: List<String>,   // read, not hardcoded — see §14.2
)

data class Account(
    val id: Long,
    val platformId: String,
    val kind: AccountKind,
    val displayEmail: String,  // "alex@example.com"
    val planName: String,      // "Max 5×"
    val planLine: String,      // "Max 5× · rolling session and weekly caps"
    val footNote: String,      // the explainer under the rule
    val signedInAt: Instant,
    val tabOrder: Int,
    val notifyOnReset: Boolean,
    val notifySession: Boolean,
    val notifyWeekly: Boolean,
    val quietHours: ClosedRange<LocalTime>?,  // work accounts — see 2b
)

data class UsageWindow(
    val id: Long,
    val accountId: Long,
    val name: String,          // "Current session", "Cursor Models"
    val usagePercent: Float,   // 0..100
    val windowStart: Instant,
    val windowEnd: Instant,
    val segments: List<UsageSegment>,  // empty unless split by use
    val sortOrder: Int,        // 0 = shortest window → outer arc on the watch
) {
    val elapsedPercent: Float
    val isBreached: Boolean get() = usagePercent >= ALERT_THRESHOLD
    val paceDelta: Float get() = usagePercent - elapsedPercent
}

data class UsageSegment(val label: String, val percent: Float, val colorIndex: Int)

const val ALERT_THRESHOLD = 90f  // Usage.dc.html: `usage >= 90`
const val PACE_TOLERANCE  = 12f  // Usage.dc.html: `ahead > 12` / `ahead < -12`
```

### 5.1 Reset-time formatting

Two distinct formats, both required.

**Phone, in `WindowRow`:**

| Condition | Format | Example |
|---|---|---|
| < 24 h, same day | relative | `Resets in 4 hr 29 min` |
| tomorrow | relative day | `Resets tomorrow 12:00 AM` |
| within the week | weekday + time | `Resets Tue 7:00 AM` |
| further out | date | `Resets 3 Sep` · `Resets Thu 3 Sep, 9:00 AM` |

**Watch, curved labels:** `name · used% · when`, dot-separated. Resets **inside 48 hours** read `Today 7:29 PM` or `Tomorrow 12:00 AM`; anything further carries the date — `Tue 1 Sep, 7:00 AM`.

The watch centre carries the **relative** countdown (`Resets in 4:29`), so the user gets clock time and time remaining together.

Put all of this in one `ResetFormatter` in `:core:model`, unit-tested across timezone and DST boundaries. It is the most bug-prone code in the app.

### 5.2 Pace copy

Shown when a phone window row is expanded. Preserve the wording:

| Condition | Copy |
|---|---|
| `paceDelta > 12` | `Ahead of the clock by N points — at this rate you run out before it resets.` |
| `paceDelta < -12` | `Behind the clock by N points. Plenty of room left in this window.` |
| otherwise | `Tracking the clock almost exactly. This window should just about last.` |

Italic, 14sp, Neutral800, behind a 2dp vertical rule in the platform bar hue — or breach red when `paceDelta > 12`.

---

## 6. Phone screens

Board card geometry is **412 × 740 dp**. Treat 412dp as the reference width.

### 6.1 Screen inventory

| ID | Screen | Route | Notes |
|---|---|---|---|
| 1a | Empty app | `Empty` | Only while zero accounts exist |
| 1b | Platform ledger | `Ledger` | Pick + Work tick |
| 1d | In-app sign-in | `SignIn(platformId, work)` | Step 2 of 2 |
| 1e | First tab | `PlatformScreen(accountId)` | Same screen as 4a–4i |
| 2a | Settings — personal | `SettingsScreen(accountId)` | |
| 2b | Settings — work | `SettingsScreen(accountId)` | Same route, work data |
| 2c | Remove confirm | dialog over settings | |
| 2d | Information | `Info` | Outside the tabs entirely |
| 4a–4i | Platform screens | `PlatformScreen(accountId)` | One route, nine data sets |

`1c`, `3b`, `3d` do not exist. They were discarded variants — no board entry, no screenshot.

### 6.2 `1a` — Empty app

Title `Usage`. Body inset 74dp from the top, max width 300dp:

- Headline 27sp/600 — *Add the subscriptions you pay for.*
- Body 14sp Neutral700 — *Each one gets its own tab along the bottom, with its session and weekly windows.*
- **Get started**: full block width, min 50dp, `Accent700` ground, Paper text, ⊕ 19sp, label 16sp/600, square corners, 26dp top margin.
- Caption 12sp Neutral600 — *Same as the plus below — this screen only shows while nothing is connected.*

Tab strip present but empty apart from the fixed **Add**. Once any account exists, this screen is unreachable forever.

### 6.3 `1b` — Platform ledger

Title `Usage`, meta `Pick what you pay for · tick Work first`. Scrolling list of all nine. Each row:

```
min 62dp tall, 13dp vertical padding, 1dp top divider
[10dp swatch] [name 18sp/600 + plans 11sp Neutral600] [Work tick] [⊕ 44dp]
```

The Work tick is a **17dp square outline** (1.5dp, Neutral500) plus `WORK` at 10sp/600 uppercase `+0.07em`. Not a Material checkbox — square, unfilled.

Tick Work *before* pressing ⊕. Adding the same platform twice, once personal and once work, is supported and expected.

Plan strings as shown on the board: Claude `Pro · Max 5× · Max 20×` · Gemini `AI Pro · AI Ultra` · Codex `Plus · Pro` · Grok `SuperGrok · Heavy` · Perplexity `Pro · Max` · Cursor `Pro · Ultra` · GitHub Copilot `Pro · Pro+` · Microsoft 365 Copilot `Personal · Business`.

**Do not hardcode these.** Plan names and tiers change fast — Grok Bot's changed twice in three weeks. Put them in a data source you can update without a release, and treat the board strings as seed values.

Tab strip shows `Nothing added yet`.

### 6.4 `1d` — In-app sign-in

Title `Add Claude` (31sp, slightly larger than standard to fill the line), meta `Step 2 of 2 · sign in`.

- Lead 15sp — *[Platform]'s own sign-in page opens inside the app. The session it hands back stays on this phone and is what reads your usage.*
- Bordered panel (1dp Divider, Surface ground, padding 12/14/16dp) framing the WebView:
  - Chrome row: 🔒 Accent700 + host (`claude.ai`) + `IN-APP` right-aligned uppercase 11sp
  - The WebView
  - Primary action bar: min 46dp, **platform bar hue** ground, `#1A1817` text, 15sp/600 — *Continue to [Platform]*
- Two assurance rows, 13sp Neutral800, Accent700 icons 17sp:
  - *Kept on the phone: the session cookie and the usage figures.*
  - *Never sent anywhere: nothing is uploaded, no password is seen.*
- Tab strip shows `Finish to get your first tab`.

### 6.5 `4a–4i` / `1e` — Platform screen

The main screen. One layout, nine data sets.

```
ScreenHeader(title = platform.displayName, icons = [refresh, gear])
   meta: "Personal account · updated 2 min ago" | "Work account · …"
Body (padding 20dp):
   planLine   13sp Neutral700, 18dp bottom padding
   for each window → WindowRow
   SectionRule
   footNote   12sp Neutral600, 14dp top margin, lh 1.6
PlatformTabStrip(mode = NORMAL)
```

Tapping a window row expands the pace read (§5.2); tapping again collapses it.

**Per-platform content** — transcribe verbatim:

| ID | Title | Plan line | Windows: name, usage%, elapsed%, reset |
|---|---|---|---|
| 4a | Claude | `Max 5× · rolling session and weekly caps` | Current session 45 / 10 / `in 4 hr 29 min` · Weekly limit 31 / 67 / `Tue 7:00 AM` |
| 4b | Gemini | `AI Pro · Pro and Flash counted separately` | Session, Pro 26 / 71 / `in 1 hr 25 min` · Weekly, Pro 18 / 33 / `Thu 3:49 PM` |
| 4c | Codex | `Included with Plus · rolling session and weekly caps` | Current session 20 / 52 / `in 2 hr 24 min` · Weekly limit 58 / 44 / `Thu 3 Sep, 9:00 AM` |
| 4d | Grok | `SuperGrok · one weekly limit, split by use` | **Weekly SuperGrok Limit 44 / 86** / `tomorrow 12:00 AM` — segments Voice 19 · Chat 5 · Coding 20 |
| 4e | Perplexity | `Pro · searches and Labs counted apart` | Pro searches, daily 37 / 52 / `in 11 hr` · Labs, monthly 14 / 96 / `1 Sep` |
| 4f | Cursor | `Pro · Cursor and other models counted apart` | **Cursor Models 94 / 96** *(breached)* / `3 Sep` · Other Models 64 / 96 / `3 Sep` |
| 4g | GitHub Copilot | `Pro · premium requests monthly` | Premium requests 52 / 96 / `1 Sep` · Chat, monthly 41 / 96 / `1 Sep` |
| 4h | Microsoft 365 (**W**) | `Business · agent credits and chat` | Copilot chat, daily 22 / 52 / `in 11 hr` · Agent credits 63 / 96 / `1 Sep` |
| 4i | Grok Bot | `Included with SuperGrok` + sub-line `Billed with your Grok subscription` | Weekly session 61 / 86 / `tomorrow 12:00 AM` |

Footnotes, verbatim:

- **4a** Session windows are five hours long and start with your first message.
- **4b** Read through the Google session already on this phone. Flash is unmetered on AI Pro.
- **4c** Local and cloud tasks draw on the same two caps. Upgrading the ChatGPT plan raises both.
- **4d** Voice, chat and coding all draw on the same weekly limit; it rolls Monday at midnight local time.
- **4e** Quick searches are unlimited; only Pro searches and Labs runs count.
- **4f** Cursor's own models draw on the included allowance; other models bill separately once it runs out.
- **4g** Completions are unlimited on Pro; premium model requests are the metered part.
- **4h** Credits are pooled across the tenant, so the figure can move without you.
- **4i** Grok Bot rides on whichever plan you hold — SuperGrok or Cursor. Its weekly session is counted on its own, separate from the parent subscription's limits.

**4i is special:** the plan line names *which* subscription is paying, because Grok Bot can arrive via SuperGrok or via Cursor. The same screen must read correctly for both. This is confirmed real behaviour, not a design invention — see §14.2.

### 6.6 `2a` / `2b` — Settings

Settings is a **mode**, not a separate screen. It rides the same tab strip in the same order, so switching accounts keeps both controls in the same physical position.

```
ScreenHeader(title = "[Platform]  settings", icons = [info, gear-lit])
   "settings" is a 15sp/400 Neutral600 suffix on the 30sp title
   meta: "Personal account" | "Work account"
Body:
   SECTION "ACCOUNT"
     [10dp swatch] [email 17sp/600, ellipsised]
                   [plan · signed-in date, 11sp Neutral600]
   SECTION "NOTIFICATION"
     "Tell me when limits reset"   16sp/600
     explainer 13sp Neutral700  +  Toggle (platform hue)
     sub-state row: "Session window on" · "Weekly window on"  12sp, 22dp gap
   SectionRule (22dp top)
   footnote
PlatformTabStrip(mode = SETTINGS)   // ⊕ becomes ⊖ "Remove" in #A81C13
```

**2b (work)** differs only in data: gold **W** before the title, `Work account` meta, work email, `Team seat · signed in 26 Aug`, both toggles **off**, explainer *Off outside working hours, so a reset at 2am stays quiet.* Work accounts carry a quiet-hours concept; personal accounts do not.

Footnotes — 2a: *Switching tabs keeps you in settings — the same two controls, per account.* 2b: *Two Claude accounts, two sets of settings — the tab decides which you are editing.*

### 6.7 `2c` — Remove confirmation

Modal over settings. Names the **exact** account so a work tab is never removed by mistake.

- Title 20sp/600 — *Remove this account?*
- Body 14sp Neutral800 — **Claude · alex@work.example.com** *loses its tab, its session is deleted from this phone, and its notifications stop. Your other Claude account is untouched.*
- Actions side by side, each `weight 1f`, min 46dp, 10dp gap:
  - **Cancel** — 1dp Neutral500 outline, transparent
  - **Remove** — `#B3231E` ground, Paper text
- Caption 11sp Neutral600 — *You can add it again from the plus at any time.*

Removal must delete the session cookie, the cached figures, and any scheduled notifications for that account. Verify with an instrumented test.

### 6.8 `2d` — Information

Its own page, no tabs, back arrow to the caller. Q&A structure — every heading is a question a user would actually ask.

```
Header: [← back] Information       meta: "Usage Ledger · 0.4.2 beta"
H3  What is this app?
H3  Design philosophy      — contains the pull-quote "am I near my limit?"
H3  Frequently asked
      Why do I sign in inside the app?
      Where is my data kept?
      Can I add the same platform twice?
      Why are my numbers a few minutes behind?
      A platform I pay for is missing.
H3  Donate to support      — [Donate] [Share]
Footer: Version 0.4.2 (build 118) · 30 Aug 2026
        "Not affiliated with any of the platforms listed.
         Names and marks belong to their owners."
```

Copy is in `Screens Board.dc.html`, screen `2d`. Transcribe verbatim into `strings.xml` — it is privacy and legal copy, not filler.

---

## 7. Wear OS

Wear is **dark-theme only**. Every face is black; platform hues are lifted to lighter, less-saturated steps.

### 7.0 Two things to settle before you start

**These are not watch faces.** The board calls screens `3a` and `w-4a`–`4i` "faces," but none displays the time. They are **activity screens inside your app**, built with Compose. Say this explicitly to any AI assistant, or it will reach for the watch-face APIs and build the wrong thing entirely — and since January 2026 the Watch Face Format is required for installing actual watch faces, which is a declarative XML format, not Compose. If you ever *do* want a real watch face, that is a separate deliverable on a different toolchain.

**Wear version mapping**, for pinning `minSdk` and reading Google's docs:

| Wear OS | Android | API |
|---|---|---|
| 3 | 11 | 30 |
| 4 | 13 | 33 |
| 5 | 14 | 34 |
| 5.1 | 15 | 35 |
| 6 | 16 | 36 |

At `minSdk 36` you are Wear OS 6 only. Confirm your hardware runs it before package 11 — see the warning in 2.1.

### 7.1 Device target

The bundle ships a working emulator skin. Install it:

```
uploads/device_bezel.png  → skin background
uploads/device_mask.png   → skin foreground mask
uploads/hardware.ini      → hw.rotaryInput=yes, hw.sensors.heart_rate=yes
uploads/layout            → 454×454 display inside a 539×539 skin, offset 42,42
```

Copy the four files into an AVD skin directory and point a Wear AVD at it. This previews at true size — do not develop against the generic round emulator.

Design coordinates use a **380 × 380** viewBox mapped onto the 454px display; scale ≈ 1.195. Content sits inside a 64dp inset safe area. Every tap target ≥ 44dp.

### 7.2 Watch inks

| Platform | Outer arc | Inner arc |
|---|---|---|
| Claude | `#E08A6B` | `#E8A78E` |
| Gemini | `#8FB4F2` | `#A6C6F6` |
| Codex | `#4FC0A3` | `#6ED0B6` |
| Grok | `#F0ECE7` — segments `#F0ECE7` · `#B5B0AA` · `#7D7873` | — |
| Perplexity | `#6FB3BD` | `#8AC6CE` |
| Cursor | `#FF5A4D` (breached, glowing) | `#B8B3AD` |
| GitHub Copilot | `#A98AE0` | `#BDA3EC` |
| Microsoft 365 | `#7FB4E8` | `#9AC6F0` |
| Grok Bot | `#F0ECE7` | — |

Chrome: arc track `#302D2B` · elapsed tick `#CFCBC6` · centre rule `#3A3634` · reset text `#D6D2CD` · secondary `#B0ABA5` · muted `#948F89` · work mark `#D9B34D` (gold, lifted from the phone's `#8A6A12`).

### 7.3 `3a` / `w-4a–4i` — Watch home

Draw with Compose Canvas. Geometry in the 380-space:

```
Centre (190, 190). Arcs start at 12 o'clock (rotate −90°).

Outer arc — the SHORTER window, the one that moves fastest:
    radius 168, stroke 12, circumference 1055.6
    sweep = usageFraction × 1055.6
Inner arc — the LONGER window:
    radius 132, stroke 9, circumference 829.4

Elapsed tick: 2px radial line, #CFCBC6, just outside its own arc,
              at angle = elapsedFraction × 360° from 12 o'clock

Curved labels (bottom semicircle, centred, startOffset 50%):
    outer path radius 151.5 — M 38.5 190 A 151.5 151.5 0 0 0 341.5 190
    inner path radius 113   — M 77 190 A 113 113 0 0 0 303 190
    Source Serif 4, 11sp, weight 600, tracking 0.4, fill = that arc's hue
    text: "name · used% · when"

Centre stack:
    [11px platform swatch] [platform name 23sp/600, −0.015em]
    figure 76sp/600, −0.045em, inner-arc hue
    rule 88 × 1px #3A3634, 14px margins
    "Resets in 4:29"  14sp #D6D2CD
```

The outer arc's label sits *lowest* on the face; the inner arc's label sits above it. Each is set on the curve of its own arc.

**Variants:**

- **4d (Grok)** — one arc drawn as three consecutive segments on the outer ring at successive rotations. No inner arc, no `Resets in` line. The centre carries the parts instead: `Voice 19% · Chat 5% · Coding 20%` at 12sp.
- **4f (Cursor, breached)** — outer arc `#FF5A4D` with `drop-shadow(0 0 7px rgba(255,90,77,.85))`; figure `#FF7A6E` with `text-shadow 0 0 14px rgba(255,90,77,.5)`. Everything else stays inert graphite. Lit "the *Tron: Ares* way" — dark face, light only where it matters.
- **4h (M365)** — gold `W` at 11sp/700 `#D9B34D` before the platform name.
- **4i (Grok Bot)** — one arc, no inner. The **Grok Bot mark takes the centre** where the swatch normally sits. Name 20sp, figure 64sp (smaller than standard), then `Resets in 9:12`, then `via SuperGrok` at 11sp `#948F89`.

Use `Modifier.drawWithCache`. Implement labels-on-a-curve with `Path` + `drawTextOnPath`; Compose has no `textPath` equivalent, so this needs a custom measure. Budget time for it.

### 7.4 `3g` — Nothing connected

The only decorated screen in the system, because it is the only screen with nothing to read.

- Copy: *Nothing connected yet* / *Add a subscription on your phone and it appears here.*
- Action: **Open on phone** → caption *Opens Usage on your phone*
- **No plus.** Signing in needs a WebView the watch cannot provide. The face points at the phone and stops.

Decoration, clipped to a circle of radius 158:

- Faint starfield — ~30 dots, radius 0.8–1.7, opacity 0.25–0.6; a few on `twinkle` loops of 3.4–4.9 s with staggered delays.
- Shooting stars on eight staggered loops, gradient trails from transparent `#7CC6DD` to `#B8E4F2`.
- Once a minute, a small saucer crosses from three o'clock to nine.

Respect `prefers-reduced-motion` — drop to a static field. Gate on ambient mode: it must not run always-on.

The tile carousel shows the same single line in place of its rows.

### 7.5 `3c` — Reset notification

The reason to wear it. The arc completes to a full ring in the platform's outer hue.

- ⟲ icon 34sp in the inner hue
- Headline 26sp/600 — *Claude session reset*
- Body 14sp `#B0ABA5` — *The full five hours are yours again.*
- **Open** pill — min 46dp, radius 23dp, platform outer hue ground, `#1A1817` text, 15sp/600

### 7.6 Tiles — ProtoLayout, not Compose

Both tiles are `androidx.wear.protolayout`, reached by swiping from the watch face. One screen, no scrolling. Both follow `PrimaryLayout`: `titleSlot`, `mainSlot`, one action in `bottomSlot`. Arcs are `ArcLine`, not composables — do not try to share drawing code with 7.3.

**`3f` — Three sessions.** Safe area inset `31/19/29`.

```
titleSlot:  "Usage"  14sp/600, +0.02em, #E8E5E1
mainSlot:   Paper-ground container, corner radius 104dp, padding 20/48/18
            — a wide, heavily rounded box that tucks its corners inside the
              screen curve, the way system tiles do
            three rows, 11dp gap. Each row:
              [8dp swatch] [optional gold W] [name 14sp/600] … [figure 17sp/600]
              UsageBar — 6dp track, hairline 3dp above/below
              [window label 10sp] … [reset 10sp]   Neutral600
bottomSlot: Refresh pill — min 44dp, radius 22dp, #25211F ground,
            #7CC6DD text, ⟳ 18sp + "REFRESH" 10.5sp/600 uppercase +0.09em
```

Which three sessions appear is the user's own pick, made on the phone.

**`3e` — One session per card.** Two cards on the face at a time, radius 26dp, padding 14/18/15, 12dp gap.

```
[11dp swatch] [platform 15sp/600] … [figure 27sp/600, −0.02em]
UsageBar — 7dp track, 9dp top margin, hairline 3dp above/below
[window name 11sp] … [reset 11sp]   Neutral700
```

Footer affordance: ⌄ caret 15sp + `2 MORE` 10.5sp/600 uppercase, `#948F89`, min 30dp. The rest are reached by swiping down.

**Both tiles keep the paper ground and full-strength phone inks while the watch stays black.** Google's tile guidance asks for black. This is a deliberate departure — record it as an accepted deviation so nobody "fixes" it later.

### 7.7 Phone ↔ watch

The watch never authenticates. Sync one way:

```
Phone → DataClient → Watch
    /accounts   account list, order, colours, work flags
    /windows    current figures, window bounds, segments
    /tilepicks  which sessions the tiles show
Watch → MessageClient → Phone
    /open       bring Usage to the foreground (3g, 3c "Open")
```

Push on every successful phone refresh. The watch renders what it last received and shows staleness in the reset line. It never fetches on its own.

---

## 8. Data acquisition

### 8.0 Status: under investigation

**This section describes the design as drawn, not a validated approach.** A nine-provider research brief (`GROK_BOT_BRIEF-data-layer.md`) is out with agents to establish, per platform, whether reading a user's own usage figures is supported through an official API, permitted but unsupported, or prohibited.

**Do not write a real parser until those findings come back.** Build packages 1–15 against fakes. The findings may cut platforms from v1 or replace the mechanism entirely; everything upstream of `:core:providers` is unaffected either way.

### 8.1 How the design assumes it works

Each provider is read by loading its usage page in a WebView carrying the session cookie captured at sign-in, then extracting the figures. Nothing leaves the phone; there is no server and no account to create.

```kotlin
interface UsageProvider {
    val platformId: String
    val signInUrl: String
    val usageUrl: String
    val cookieDomain: String
    suspend fun parse(page: String): List<UsageWindow>
    fun isSessionValid(page: String): Boolean
}
```

One implementation per platform in `:core:providers`, registered in a map keyed by `platformId`. Adding a tenth platform must mean adding one file and one map entry.

### 8.2 Risks

This is the highest-risk part of the project, and the outline would be dishonest not to say so.

| Risk | Mitigation |
|---|---|
| **Terms of service.** Automated reading of a provider's pages may breach its ToS even with the user's own session. | Under investigation. Anything ambiguous goes to a lawyer, not to an AI assistant. |
| **Brittleness.** Any provider redesign breaks the parser silently. | Never crash on a parse failure. Show a per-account "couldn't read" state, keep the last good figures with their timestamp, log a structured parse error. Version each parser and pin an expected-shape fixture. |
| **Session expiry.** Cookies expire; some providers rotate aggressively. | Detect via `isSessionValid`, mark the account as needing re-auth, surface a re-sign-in affordance. Never show stale numbers as current. |
| **Bot detection.** Headless-looking requests may be challenged. | Load in a real WebView with a normal user agent. If challenged, hand the user the WebView rather than trying to solve it. |
| **Cookie security.** Session cookies are credentials. | `EncryptedSharedPreferences` or the Keystore. Never log them, never include them in crash reports or `adb` output, wipe on account removal. |
| **Silent tenant changes (4h).** M365 credits are pooled, so figures move without the user. | The footnote already says so. Do not treat unexpected jumps as parse errors. |

### 8.3 Fallback — design it now

Every platform that does not come back SANCTIONED needs a manual-entry path. The app must degrade to something useful rather than showing an empty tab.

A window needs only a percentage and a reset time to render correctly, so a manual account is a first-class citizen: the user types the figure they see on the provider's own page, and the bar, hairline, pace copy, watch arc and reset notification all work unchanged. Build `UsageWindow` so nothing downstream knows or cares where the number came from.

### 8.4 Refresh

Cadence is a user setting: **On open · Hourly · Daily** (from `Usage.dc.html`). Manual refresh via the header ⟳. Use WorkManager with a network constraint and exponential backoff. Never refresh on a metered connection without consent.

---

## 9. Notifications

One notification the moment a session or weekly window is **clear again** — a reset, not a warning. Per account, per window type, individually switchable.

- One channel per account, so the user can silence a single platform from system settings.
- `POST_NOTIFICATIONS` is a runtime permission at this API level. Request it at the point the user first enables a reset notification, not at launch.
- Work accounts honour quiet hours: *Off outside working hours, so a reset at 2am stays quiet.*
- Mirror to the watch as `3c`.
- Schedule with `AlarmManager` at the known `windowEnd`; re-arm on each refresh.
- Cancel everything for an account on removal.

Breach is **not** notified in v1. Red on the bar is the only breach signal.

---

## 10. Navigation and state

Type-safe Navigation Compose with `@Serializable` routes. No string routes.

```kotlin
@Serializable data object Empty
@Serializable data object Ledger
@Serializable data class  SignIn(val platformId: String, val work: Boolean)
@Serializable data class  PlatformScreen(val accountId: Long)
@Serializable data class  SettingsScreen(val accountId: Long)
@Serializable data object Info
```

Rules:

- Zero accounts → `Empty` is the start destination. One or more → `PlatformScreen(firstTab)`.
- `Get started` (1a) and `⊕` (tab strip) are the **same action** → `Ledger`.
- Settings is a mode flag on the platform screen, not a nested graph. Switching tabs preserves the flag.
- `Info` sits outside the tab hierarchy; back returns to the caller, which may be settings or a platform screen.
- `⊖` opens the remove dialog over the current settings screen.

State: one `ViewModel` per screen, one `UiState` sealed interface each, collected with `collectAsStateWithLifecycle()`. Composables take state and lambdas — never a `ViewModel`.

---

## 11. Build sequence

| # | Package | Depends on | Exit criteria |
|---|---|---|---|
| 1 | Skeleton, version catalog, module graph, CI | — | Phone + wear apps install; CI green; `NewApi` lint on as error |
| 2 | Broadsheet tokens, Source Serif 4, phone theme | 1 | Dynamic colour and dark theme removed; token previews render |
| 3 | `UsageBar` + `WindowRow` | 2 | All §4.1 previews correct, including segmented and breached |
| 4 | `PlatformTabStrip`, `ScreenHeader`, `Toggle`, swatches, icons | 2 | Previews for normal and settings modes |
| 5 | Domain model, `ResetFormatter`, fake repositories | 1 | Formatter tests pass across DST and timezones |
| 6 | Navigation graph, stub destinations | 5 | Every route reachable; back stack correct |
| 7 | Platform screen (4a–4i) against fakes | 3,4,5,6 | All nine match their PNGs; 4d and 4f correct |
| 8 | Onboarding (1a, 1b, 1d shell, 1e) | 7 | Flow completes with a fake provider |
| 9 | Settings + remove (2a, 2b, 2c) | 7 | Mode rides the tab strip; removal wipes everything |
| 10 | Information (2d) | 6 | Copy verbatim; Donate and Share wired |
| 11 | Wear skin, dark theme, arc primitives | 2 | AVD at 454px; **runs on your actual watch**; arcs at correct radii |
| 12 | Watch home (3a, w-4a–4i) | 11,5 | Curved labels correct; 4d/4f/4i variants correct |
| 13 | Tiles (3f, 3e) — ProtoLayout | 11 | Both render in the carousel; Refresh works |
| 14 | Watch empty (3g), notification (3c) | 12 | Decoration respects reduced motion and ambient |
| 15 | DataLayer bridge | 12,13 | Watch reflects phone within seconds |
| 16 | **Data-layer findings review** | brief returned | Per-platform verdict; ship list agreed; §8 rewritten |
| 17 | Manual-entry fallback (§8.3) | 16, 8 | A manual account renders identically to a read one |
| 18 | First real provider, end to end | 16, 17 | Real figures on both devices |
| 19 | Remaining sanctioned providers | 18 | Each fails gracefully on a mangled fixture |
| 20 | Notifications + WorkManager | 18 | Reset fires on time; quiet hours honoured |
| 21 | Accessibility, localisation, config passes | 14 | Scanner clean; 200% font scale usable |
| 22 | Screenshot baselines, R8, signing | 21 | Release build; no R8 runtime crashes |

Packages 7–10 and 11–14 can run in parallel by two people once 5 and 6 land. **Package 16 is a gate** — nothing from 17 onward starts until the findings are read.

---

## 12. Definition of done — per screen

1. Renders within tolerance of its source PNG at 412dp (phone) or 454px (watch).
2. Uses only `:core:designsystem` and `:core:ui` — no ad-hoc colours, dimensions, or text styles.
3. Loading, empty, content, and error states modelled and previewable.
4. `ViewModel` unit-tested for every state transition.
5. Compose UI test asserts each state's content.
6. Screenshot baseline committed (Roborazzi or Paparazzi).
7. TalkBack traversal is logical, every control labelled; the usage bar announces *"Claude, current session, 45 percent used, window 10 percent elapsed."*
8. No hardcoded user-facing strings.
9. Correct under rotation, process death, and 200% font scale.
10. Zero new lint or detekt warnings.
11. Any direct platform API above level 30 recorded in `COMPAT.md`.

---

## 13. Prompt patterns for the AI assistant

Constraints outperform goals. Restate these at the start of every session:

> **Standing rules for this project**
> - Broadsheet is a **light, square-cornered, serif** system. Radii are 1–2dp. Do not apply Material 3 defaults, do not round corners, do not add a dark theme to the phone app, do not use dynamic colour.
> - Source Serif 4 for everything, headings and body alike.
> - Cyan `#0088B0` is the only interactive colour. Red only on breach. Gold only on work accounts.
> - Usage figures use tabular numerals.
> - The Wear screens are **app screens**, not system watch faces. Never use the watch-face APIs.
> - `minSdk` is 36 as a temporary prototype floor. Prefer AndroidX wrappers over direct platform APIs so it can be lowered later. Record any direct platform call above API 30 in `COMPAT.md`.
> - Do not add dependencies outside the version catalog.
> - Do not invent design tokens. If a value is missing, stop and ask.
> - Do not modify files outside the module you were asked to change.

**Screen implementation**

> Implement `<ScreenName>` as a stateless composable plus a `ViewModel`.
> Use only components from `:core:ui` (§4). State is a sealed interface with Loading, Empty, Content, Error. The composable takes `uiState` and event lambdas — never the ViewModel. Preview every state.
> Spec: [paste the §6.x subsection]. Reference: `screenshots/<id>.png`.

**The usage bar** — write this one by hand, or review it line by line. It is the product.

**Watch arcs**

> Draw with Compose Canvas in a 380×380 coordinate space scaled to the target size. Arcs start at 12 o'clock. Outer radius 168 stroke 12; inner radius 132 stroke 9. Do not use Wear's `CircularProgressIndicator` — the geometry, the elapsed tick, and the curved labels are all custom.

**Review pass**

> Review this file against the standing rules and §12. List violations only. Do not rewrite. If a rule is not violated, say nothing about it.

---

## 14. Discrepancies and open questions

### 14.1 Cursor's colour — resolved, but note it

The print review's colour table lists Cursor as `#D1201A` / `#8F1410` (brand red). The board contradicts this: screen `4f` uses graphite, screen `1b`'s ledger dot is `#7D7979`, tile `3e` uses `#7D7979`, and the `4f` caption states *"Cursor keeps its graphite."*

**Follow the board: graphite.** The print table row is stale, and its red sits one shade from the breach red, which would make a breached Cursor window nearly indistinguishable from a healthy one. Correct the table before the design record circulates.

### 14.2 Grok Bot — real product, unresolved colour

Confirmed as a genuine, distinct xAI product, not a design invention. It launched in beta on 11 August 2026: persistent agents, each with its own cloud computer, own logins and own always-on runtime, which sign in to a company's software and carry out multi-step tasks unsupervised. Access at launch was via SuperGrok Heavy, Cursor Ultra and Cursor Premium Teams.

The dual billing parentage on screen `4i` is therefore accurate. **The colour is not settled.** Grok Bot currently shares Grok's black (`#201E1D`) on the phone and `#F0ECE7` on the watch. Two adjacent tabs with the same hue breaks the rule that a colour means a platform — and these are genuinely different products from the same company, not two views of one. Assign Grok Bot its own step, or accept the collision knowingly.

**Plan names are volatile.** Launch coverage lists $300 / $200 / $120-per-seat with no free tier. Coverage three weeks later describes a free trial, Pro+ at $60/month, and linking an existing SuperGrok subscription at no extra charge. Do not hardcode tier names anywhere — see 6.3.

### 14.3 Needs a decision before build

1. **Alert threshold.** `Usage.dc.html` uses 90%. Screen `4f` shows 94% as "past the alert threshold." 90% is consistent with both — confirm, and confirm whether it should be user-configurable.
2. **Quiet hours.** `2b` implies work accounts are working-hours aware, but no screen defines the hours. Add a control, or hardcode a default and note it.
3. **Tile picker.** Both tiles say their contents are "the user's own pick, made on the phone." That phone screen does not exist on the board. It must be designed before package 13.
4. **Donate and Share (2d).** Which payment rail? Play Billing has policy implications for donations.
5. **Grok Bot parentage — partly answered.** SpaceX closed its acquisition of Cursor on 14 August 2026, and the two products now share one account and billing system rather than being cross-company partners. If that holds, a single Cursor sign-in may cover the Cursor tab, the Grok tab and the Grok Bot tab, and the app could read the parent plan from that session rather than asking. **Verify against the live account model before building separate auth paths** — this is on the agent brief.
6. **Work-account detection.** `1b` has the user tick Work manually. Is that the only signal, or is it inferred?
7. **Tab overflow.** Nine platforms × two account kinds = up to 18 tabs. Confirm the scrolling strip holds at that count, or design an overflow.
8. **Codex vs ChatGPT.** `Usage.dc.html` had a ChatGPT tab; the board replaced it with Codex. Confirm ChatGPT is intentionally absent.
9. **Always-on display.** No ambient variant is designed. The board's "try next" list names it as future work. Decide whether v1 ships without one.
10. **Watch hardware.** Which Wear OS version does your device run? Governs `:wear` `minSdk` and blocks package 11.

---

## Appendix — Traceability

| Screen | Source PNG | Board anchor | Section |
|---|---|---|---|
| 1a, 1b, 1d, 1e | `p-1a/1b/1d/1e.png` | `#1a` … `#1e` | 6.2–6.5 |
| 2a, 2b, 2c, 2d | `p-2a/2b/2c/2d.png` | `#2a` … `#2d` | 6.6–6.8 |
| 4a–4i | `p-4a` … `p-4i.png` | `#4a` … `#4i` | 6.5 |
| 3a, 3c, 3e, 3f, 3g | `w-3a/3c/3e/3f/3g.png` | `#3a` … `#3g` | 7.3–7.6 |
| w-4a–4i | `w-4a` … `w-4i.png` | `#4a` … `#4i` | 7.3 |

**Companion document:** `GROK_BOT_BRIEF-data-layer.md` — the nine-provider research brief gating Section 8.
