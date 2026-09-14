package com.ericmbpeck.ai_sum.data

import com.ericmbpeck.ai_sum.model.Account
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.model.UsageSegment
import com.ericmbpeck.ai_sum.model.UsageWindow

fun fakeAccount(platformId: String, id: Long, kind: AccountKind): Account? = when (platformId) {
    Platforms.CLAUDE -> fakeClaudeAccount(id, kind)
    Platforms.GEMINI -> fakeGeminiAccount(id, kind)
    Platforms.CODEX -> fakeCodexAccount(id, kind)
    Platforms.GROK -> fakeGrokAccount(id, kind)
    Platforms.PERPLEXITY -> fakePerplexityAccount(id, kind)
    Platforms.CURSOR -> fakeCursorAccount(id, kind)
    Platforms.COPILOT -> fakeCopilotAccount(id, kind)
    Platforms.M365 -> fakeM365Account(id, kind)
    Platforms.GROK_BOT -> fakeGrokBotAccount(id, kind)
    else -> null
}

fun fakeClaudeAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.CLAUDE,
    kind = kind,
    planLine = "Max 5× · rolling session and weekly caps",
    footNote = "Session windows are five hours long and start with your first message.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Current session",
            usagePercent = 45f,
            elapsedPercent = 10f,
            resetLabel = "Resets in 4 hr 29 min",
            elapsedLabel = "Window 10% elapsed",
        ),
        UsageWindow(
            name = "Weekly limit",
            usagePercent = 31f,
            elapsedPercent = 67f,
            resetLabel = "Resets Tue 7:00 AM",
            elapsedLabel = "Window 67% elapsed",
        ),
    ),
)

fun fakeGeminiAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.GEMINI,
    kind = kind,
    planLine = "AI Pro · Pro and Flash counted separately",
    footNote = "Read through the Google session already on this phone. Flash is unmetered on AI Pro.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Session, Pro",
            usagePercent = 26f,
            elapsedPercent = 71f,
            resetLabel = "Resets in 1 hr 25 min",
            elapsedLabel = "Window 71% elapsed",
        ),
        UsageWindow(
            name = "Weekly, Pro",
            usagePercent = 18f,
            elapsedPercent = 33f,
            resetLabel = "Resets Thu 3:49 PM",
            elapsedLabel = "Window 33% elapsed",
        ),
    ),
)

fun fakeCodexAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.CODEX,
    kind = kind,
    planLine = "Included with Plus · rolling session and weekly caps",
    footNote = "Local and cloud tasks draw on the same two caps. Upgrading the ChatGPT plan raises both.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Current session",
            usagePercent = 20f,
            elapsedPercent = 52f,
            resetLabel = "Resets in 2 hr 24 min",
            elapsedLabel = "Window 52% elapsed",
        ),
        UsageWindow(
            name = "Weekly limit",
            usagePercent = 58f,
            elapsedPercent = 44f,
            resetLabel = "Resets Thu 3 Sep, 9:00 AM",
            elapsedLabel = "Window 44% elapsed",
        ),
    ),
)

fun fakeGrokAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.GROK,
    kind = kind,
    planLine = "SuperGrok · one weekly limit, split by use",
    footNote = "Voice, chat and coding all draw on the same weekly limit; it rolls Monday at midnight local time — this one is nearly through its week with most of the allowance unused.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Weekly SuperGrok Limit",
            usagePercent = 44f,
            elapsedPercent = 86f,
            resetLabel = "Resets tomorrow 12:00 AM",
            elapsedLabel = "Window 86% elapsed",
            segments = listOf(
                UsageSegment("Voice", 19f, 0xFF201E1D),
                UsageSegment("Chat", 5f, 0xFF56524F),
                UsageSegment("Coding", 20f, 0xFF8C8783),
            ),
        ),
    ),
)

fun fakeGrokBotAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.GROK_BOT,
    kind = kind,
    planLine = "Included with SuperGrok\nBilled with your Grok subscription",
    footNote = "Grok Bot rides on whichever plan you hold — SuperGrok or Cursor. Its weekly session is counted on its own, separate from the parent subscription's limits.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Weekly session",
            usagePercent = 61f,
            elapsedPercent = 86f,
            resetLabel = "Resets tomorrow 12:00 AM",
            elapsedLabel = "Window 86% elapsed",
        ),
    ),
)

fun fakePerplexityAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.PERPLEXITY,
    kind = kind,
    planLine = "Pro · searches and Labs counted apart",
    footNote = "Quick searches are unlimited; only Pro searches and Labs runs count.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Pro searches, daily",
            usagePercent = 37f,
            elapsedPercent = 52f,
            resetLabel = "Resets in 11 hr",
            elapsedLabel = "Window 52% elapsed",
        ),
        UsageWindow(
            name = "Labs, monthly",
            usagePercent = 14f,
            elapsedPercent = 96f,
            resetLabel = "Resets 1 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
    ),
)

fun fakeCursorAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.CURSOR,
    kind = kind,
    planLine = "Pro · Cursor and other models counted apart",
    footNote = "Cursor's own models draw on the included allowance; other models bill separately once it runs out.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Cursor Models",
            usagePercent = 94f,
            elapsedPercent = 96f,
            resetLabel = "Resets 3 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
        UsageWindow(
            name = "Other Models",
            usagePercent = 64f,
            elapsedPercent = 96f,
            resetLabel = "Resets 3 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
    ),
)

fun fakeCopilotAccount(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.COPILOT,
    kind = kind,
    planLine = "Pro · premium requests monthly",
    footNote = "Completions are unlimited on Pro; premium model requests are the metered part.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Premium requests",
            usagePercent = 52f,
            elapsedPercent = 96f,
            resetLabel = "Resets 1 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
        UsageWindow(
            name = "Chat, monthly",
            usagePercent = 41f,
            elapsedPercent = 96f,
            resetLabel = "Resets 1 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
    ),
)

fun fakeM365Account(id: Long, kind: AccountKind): Account = Account(
    id = id,
    platformId = Platforms.M365,
    kind = kind,
    planLine = "Business · agent credits and chat",
    footNote = "Credits are pooled across the tenant, so the figure can move without you.",
    updatedLabel = "updated 2 min ago",
    windows = listOf(
        UsageWindow(
            name = "Copilot chat, daily",
            usagePercent = 22f,
            elapsedPercent = 52f,
            resetLabel = "Resets in 11 hr",
            elapsedLabel = "Window 52% elapsed",
        ),
        UsageWindow(
            name = "Agent credits",
            usagePercent = 63f,
            elapsedPercent = 96f,
            resetLabel = "Resets 1 Sep",
            elapsedLabel = "Window 96% elapsed",
        ),
    ),
)

internal fun Account.withBoardIdentity(): Account {
    val personalShort = planLine.substringBefore(" · ").substringBefore('\n')
    return copy(
        email = if (kind == AccountKind.WORK) {
            "alex@work.example.com"
        } else {
            "alex@example.com"
        },
        accountDetailLine = if (kind == AccountKind.WORK) {
            "Team seat · signed in 26 Aug"
        } else {
            "$personalShort · signed in 12 Aug"
        },
        notifyOnReset = kind != AccountKind.WORK,
    )
}
