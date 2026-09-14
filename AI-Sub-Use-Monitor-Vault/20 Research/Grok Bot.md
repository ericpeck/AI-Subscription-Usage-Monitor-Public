---
platform: Grok Bot
board: 4i
researched: 2026-09-09
updated: 2026-09-09
status: researched
---

# Grok Bot usage

**Researched 2026-09-09 (Cursor-billed path only).** Rule for the app: [[Grok-bot-usage-data-layer]]. SuperGrok-parented Grok Bot is still unverified — stay fake until that surface is read.

Grok Bot (`4i`) is not SuperGrok (`4d`). Do not send Grok Bot through grok.com Usage.

## 1. The data source

### Sanctioned surfaces (human)

- **Cursor-billed:** [https://cursor.com/dashboard/spending](https://cursor.com/dashboard/spending) — same page as Cursor `4f`. Exhibit `AndroidStudioScreenshots/Exibit1.png` (2026-09-09): a **Grok Bot** block with **Weekly usage 1% used**, **Resets Sep 13 (4 days left)**. On-demand spend is a separate disabled card; do not invent a second Grok Bot window from it.
- **SuperGrok-billed:** not observed on device in this pass. Board `4i` copy is still `Included with SuperGrok`. Do not guess a grok.com endpoint for the bot.

There is no official personal “remaining %” API for Grok Bot.

## 2. How authentication works

Cursor-billed Grok Bot uses the **existing Cursor WebView session** (Google-as-IdP on cursor.com, shared `CookieManager`). Plus on `4i` must **not** open Cursor `1d`. Remove Grok Bot must **not** expire cursor.com cookies — that session belongs to the Cursor tab.

## 3. What we will and will not do

Shipping (see [[Grok-bot-usage-data-layer]]): when Cursor is live, read the spending page’s Grok Bot weekly row onto native `4i`. Keep Grok Bot **off** Cursor `4f` (two pools only: Cursor Models / Other Models).

We will not:

- open a second Google login for Grok Bot
- auto-add a Grok Bot tab when Cursor signs in
- parse Grok Bot onto `4f`
- treat SuperGrok Usage as the Grok Bot meter
- call `api2.cursor.sh` or lift cookies into OkHttp

## Sources

- Eric, 2026-09-09: Cursor spending screenshot `Exibit1.png` (filename spelling as dropped).
- Cursor live path: [[Cursor]], [[Cursor-usage-data-layer]].
