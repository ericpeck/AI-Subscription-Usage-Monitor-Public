package com.ericmbpeck.ai_sum.data

import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.model.UsageSegment
import com.ericmbpeck.ai_sum.model.UsageWindow
import com.ericmbpeck.ai_sum.usage.CursorUsageMapper
import com.ericmbpeck.ai_sum.usage.UsageReadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAccountStoreTest {
    @Before
    fun reset() {
        FakeAccountStore.detach()
    }

    @Test
    fun startsEmpty() {
        assertTrue(FakeAccountStore.accounts.value.isEmpty())
    }

    @Test
    fun addingCursorLeavesEmptyUnreachable() {
        val account = FakeAccountStore.addCursor(work = false)
        assertEquals(Platforms.CURSOR, account.platformId)
        assertEquals(AccountKind.PERSONAL, account.kind)
        assertEquals(1, FakeAccountStore.accounts.value.size)
        assertEquals(94f, account.windows[0].usagePercent)
        assertEquals(64f, account.windows[1].usagePercent)
    }

    @Test
    fun workTickCreatesWorkAccount() {
        val account = FakeAccountStore.addCursor(work = true)
        assertEquals(AccountKind.WORK, account.kind)
    }

    @Test
    fun addingClaudeUsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.CLAUDE, work = false)
        assertEquals(45f, account.windows[0].usagePercent)
        assertEquals(31f, account.windows[1].usagePercent)
        assertEquals("Resets in 4 hr 29 min", account.windows[0].resetLabel)
    }

    @Test
    fun addingGeminiUsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.GEMINI, work = false)
        assertEquals(Platforms.GEMINI, account.platformId)
        assertEquals(26f, account.windows[0].usagePercent)
        assertEquals(18f, account.windows[1].usagePercent)
        assertEquals("Resets in 1 hr 25 min", account.windows[0].resetLabel)
        assertEquals("Resets Thu 3:49 PM", account.windows[1].resetLabel)
    }

    @Test
    fun addingGrokSplitsWeeklyLimitIntoSegments() {
        val account = FakeAccountStore.add(Platforms.GROK, work = false)
        val window = account.windows.single()
        assertEquals(44f, window.usagePercent)
        assertEquals(86f, window.elapsedPercent)
        assertEquals(3, window.segments.size)
        assertEquals(
            44.0,
            window.segments.sumOf { it.percent.toDouble() },
            0.01,
        )
        assertEquals("Voice", window.segments[0].label)
        assertEquals("Chat", window.segments[1].label)
        assertEquals("Coding", window.segments[2].label)
    }

    @Test
    fun addingGrokBotUsesOwnWeeklySession() {
        val account = FakeAccountStore.add(Platforms.GROK_BOT, work = false)
        val window = account.windows.single()
        assertEquals(Platforms.GROK_BOT, account.platformId)
        assertEquals(61f, window.usagePercent)
        assertEquals(86f, window.elapsedPercent)
        assertTrue(window.segments.isEmpty())
    }

    @Test
    fun addingCodexUsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.CODEX, work = false)
        assertEquals(20f, account.windows[0].usagePercent)
        assertEquals(58f, account.windows[1].usagePercent)
        assertEquals("Resets Thu 3 Sep, 9:00 AM", account.windows[1].resetLabel)
    }

    @Test
    fun addingPerplexityUsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.PERPLEXITY, work = false)
        assertEquals(37f, account.windows[0].usagePercent)
        assertEquals(14f, account.windows[1].usagePercent)
        assertEquals("Resets in 11 hr", account.windows[0].resetLabel)
    }

    @Test
    fun addingCopilotUsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.COPILOT, work = false)
        assertEquals(52f, account.windows[0].usagePercent)
        assertEquals(41f, account.windows[1].usagePercent)
    }

    @Test
    fun addingM365UsesBoardFigures() {
        val account = FakeAccountStore.add(Platforms.M365, work = true)
        assertEquals(AccountKind.WORK, account.kind)
        assertEquals(22f, account.windows[0].usagePercent)
        assertEquals(63f, account.windows[1].usagePercent)
        assertEquals("Copilot chat, daily", account.windows[0].name)
    }

    @Test
    fun everyAddableCatalogPlatformAddsFakeBoardData() {
        val addable = Platforms.catalog.filter { it.addableThisSlice }
        addable.forEach { platform ->
            val account = FakeAccountStore.add(platform.id, work = false)
            assertEquals(platform.id, account.platformId)
            assertTrue(account.windows.isNotEmpty())
        }
        assertEquals(5, FakeAccountStore.accounts.value.size)
        assertTrue(addable.none { it.id == Platforms.GEMINI })
        assertTrue(addable.none { it.id == Platforms.PERPLEXITY })
        assertTrue(addable.none { it.id == Platforms.COPILOT })
        assertTrue(addable.none { it.id == Platforms.M365 })
    }

    @Test
    fun addLiveClaudeIsNotFakeBoardFigures() {
        val windows = sampleWindows()
        val account = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = true,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = windows,
        )
        assertTrue(account.liveRead)
        assertEquals(AccountKind.WORK, account.kind)
        assertEquals(false, account.notifyOnReset)
        assertEquals(12f, account.windows[0].usagePercent)
        FakeAccountStore.markNeedsReauth(account.id)
        assertEquals(true, FakeAccountStore.account(account.id)?.needsReauth)
    }

    @Test
    fun boardIdentitySeedsPersonalEmailAndNotifyOn() {
        val account = FakeAccountStore.add(Platforms.CLAUDE, work = false)
        assertEquals("alex@example.com", account.email)
        assertEquals(true, account.notifyOnReset)
        assertEquals("Max 5× · signed in 12 Aug", account.accountDetailLine)
    }

    @Test
    fun boardIdentitySeedsWorkEmailAndNotifyOff() {
        val account = FakeAccountStore.add(Platforms.CLAUDE, work = true)
        assertEquals(AccountKind.WORK, account.kind)
        assertEquals("alex@work.example.com", account.email)
        assertEquals(false, account.notifyOnReset)
        assertEquals("Team seat · signed in 26 Aug", account.accountDetailLine)
    }

    @Test
    fun secondAddOfSamePlatformThrows() {
        FakeAccountStore.add(Platforms.CLAUDE, work = false)
        var thrown = false
        try {
            FakeAccountStore.add(Platforms.CLAUDE, work = true)
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
        assertEquals(1, FakeAccountStore.accounts.value.size)
    }

    @Test
    fun accountForReturnsTheSingleRow() {
        val claude = FakeAccountStore.add(Platforms.CLAUDE, work = false)
        assertEquals(claude.id, FakeAccountStore.accountFor(Platforms.CLAUDE)?.id)
        assertEquals(null, FakeAccountStore.accountFor(Platforms.CURSOR))
    }

    @Test
    fun removeReturnsNeighborAndLeavesTheOtherPlatform() {
        val claude = FakeAccountStore.add(Platforms.CLAUDE, work = false)
        val cursor = FakeAccountStore.add(Platforms.CURSOR, work = false)
        val next = FakeAccountStore.remove(claude.id)
        assertEquals(cursor.id, next?.id)
        assertEquals(1, FakeAccountStore.accounts.value.size)
        assertEquals(cursor.id, FakeAccountStore.accounts.value.single().id)
    }

    @Test
    fun removeLastReturnsNull() {
        val account = FakeAccountStore.add(Platforms.CURSOR, work = false)
        val next = FakeAccountStore.remove(account.id)
        assertEquals(null, next)
        assertTrue(FakeAccountStore.accounts.value.isEmpty())
    }

    @Test
    fun setNotifyOnResetTogglesOnlyThatAccount() {
        val claude = FakeAccountStore.add(Platforms.CLAUDE, work = false)
        val cursor = FakeAccountStore.add(Platforms.CURSOR, work = false)
        FakeAccountStore.setNotifyOnReset(claude.id, false)
        FakeAccountStore.setNotifyOnReset(cursor.id, true)
        assertEquals(false, FakeAccountStore.account(claude.id)?.notifyOnReset)
        assertEquals(true, FakeAccountStore.account(cursor.id)?.notifyOnReset)
    }

    @Test
    fun liveClaudeReauthFlagsEveryLiveClaudeTab() {
        val live = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = true,
            planLine = "Team · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        val cursor = FakeAccountStore.add(Platforms.CURSOR, work = false)
        FakeAccountStore.markLiveClaudeNeedsReauth()
        assertEquals(true, FakeAccountStore.account(live.id)?.needsReauth)
        assertEquals(false, FakeAccountStore.account(cursor.id)?.needsReauth)
        FakeAccountStore.clearLiveClaudeReauth()
        assertEquals(false, FakeAccountStore.account(live.id)?.needsReauth)
        assertEquals(AccountKind.WORK, FakeAccountStore.account(live.id)?.kind)
    }

    @Test
    fun hasLiveClaudeFollowsRemove() {
        val live = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = false,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        assertTrue(FakeAccountStore.hasLiveClaude())
        FakeAccountStore.remove(live.id)
        assertEquals(false, FakeAccountStore.hasLiveClaude())
    }

    @Test
    fun removingLiveClaudeLeavesLiveGrok() {
        val claude = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = false,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        val grok = FakeAccountStore.addLive(
            platformId = Platforms.GROK,
            work = false,
            planLine = "SuperGrok · one weekly limit, split by use",
            footNote = "Voice, chat and coding all draw on the same weekly limit.",
            windows = sampleGrokWindow(),
        )
        FakeAccountStore.remove(claude.id)
        assertEquals(false, FakeAccountStore.hasLiveClaude())
        assertTrue(FakeAccountStore.hasLive(Platforms.GROK))
        assertEquals(grok.id, FakeAccountStore.account(grok.id)?.id)
    }

    @Test
    fun secondLiveAddOfSamePlatformThrows() {
        FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = false,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        var thrown = false
        try {
            FakeAccountStore.addLive(
                platformId = Platforms.CLAUDE,
                work = true,
                planLine = "Team · rolling session and weekly caps",
                footNote = "Session windows are five hours long and start with your first message.",
                windows = sampleWindows(),
            )
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
        assertEquals(1, FakeAccountStore.accounts.value.size)
    }

    @Test
    fun liveGrokReauthDoesNotFlagClaude() {
        val grok = FakeAccountStore.addLive(
            platformId = Platforms.GROK,
            work = false,
            planLine = "SuperGrok · one weekly limit, split by use",
            footNote = "Voice, chat and coding all draw on the same weekly limit.",
            windows = sampleGrokWindow(),
        )
        val claude = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = false,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        FakeAccountStore.markLiveNeedsReauth(Platforms.GROK)
        assertEquals(true, FakeAccountStore.account(grok.id)?.needsReauth)
        assertEquals(false, FakeAccountStore.account(claude.id)?.needsReauth)
        FakeAccountStore.clearLiveReauth(Platforms.GROK)
        assertEquals(false, FakeAccountStore.account(grok.id)?.needsReauth)
    }

    @Test
    fun hasLiveWebSessionUntilLastClaudeGrokOrCursorIsGone() {
        val grok = FakeAccountStore.addLive(
            platformId = Platforms.GROK,
            work = false,
            planLine = "SuperGrok · one weekly limit, split by use",
            footNote = "Voice, chat and coding all draw on the same weekly limit.",
            windows = sampleGrokWindow(),
        )
        val claude = FakeAccountStore.addLive(
            platformId = Platforms.CLAUDE,
            work = false,
            planLine = "Pro · rolling session and weekly caps",
            footNote = "Session windows are five hours long and start with your first message.",
            windows = sampleWindows(),
        )
        val cursor = FakeAccountStore.addLive(
            platformId = Platforms.CURSOR,
            work = false,
            planLine = "Pro · Cursor and other models counted apart",
            footNote = "Cursor's own models draw on the included allowance; other models bill separately once it runs out.",
            windows = sampleWindows(),
        )
        FakeAccountStore.add(Platforms.CODEX, work = false)
        assertTrue(FakeAccountStore.hasLiveWebSession())
        FakeAccountStore.remove(claude.id)
        assertTrue(FakeAccountStore.hasLiveWebSession())
        FakeAccountStore.remove(grok.id)
        assertTrue(FakeAccountStore.hasLiveWebSession())
        FakeAccountStore.remove(cursor.id)
        assertEquals(false, FakeAccountStore.hasLiveWebSession())
    }

    @Test
    fun addLiveGrokIsNotFakeBoardFigures() {
        val windows = sampleGrokWindow()
        val account = FakeAccountStore.addLive(
            platformId = Platforms.GROK,
            work = false,
            planLine = "SuperGrok · one weekly limit, split by use",
            footNote = "Voice, chat and coding all draw on the same weekly limit.",
            windows = windows,
        )
        assertTrue(account.liveRead)
        assertEquals(Platforms.GROK, account.platformId)
        assertEquals(12f, account.windows[0].usagePercent)
        assertEquals("Coding", account.windows[0].segments.single().label)
    }

    @Test
    fun addGrokBotFromCursorWithoutLiveCursorStaysNull() {
        assertNull(FakeAccountStore.addGrokBotFromCursor(work = false))
        val fake = FakeAccountStore.add(Platforms.GROK_BOT, work = false)
        assertEquals(61f, fake.windows.single().usagePercent)
        assertEquals(false, fake.liveRead)
    }

    @Test
    fun addGrokBotFromCursorUsesSpendingSnapshotNotBoardFake() {
        FakeAccountStore.addLive(
            platformId = Platforms.CURSOR,
            work = false,
            planLine = "Pro+ · Cursor and other models counted apart",
            footNote = CursorUsageMapper.FOOTNOTE,
            windows = sampleWindows(),
        )
        FakeAccountStore.rememberGrokBotFromCursor(sampleGrokBotFromCursor())
        val bot = FakeAccountStore.addGrokBotFromCursor(work = false)
        requireNotNull(bot)
        assertTrue(bot.liveRead)
        assertEquals(1f, bot.windows.single().usagePercent)
        assertEquals(CursorUsageMapper.GROK_BOT_WINDOW, bot.windows.single().name)
        assertEquals(CursorUsageMapper.GROK_BOT_PLAN_VIA_CURSOR, bot.planLine)
    }

    @Test
    fun rememberGrokBotFromCursorConvertsExistingFakeTab() {
        val fake = FakeAccountStore.add(Platforms.GROK_BOT, work = false)
        FakeAccountStore.addLive(
            platformId = Platforms.CURSOR,
            work = false,
            planLine = "Pro+ · Cursor and other models counted apart",
            footNote = CursorUsageMapper.FOOTNOTE,
            windows = sampleWindows(),
        )
        FakeAccountStore.rememberGrokBotFromCursor(sampleGrokBotFromCursor())
        val updated = FakeAccountStore.account(fake.id)
        assertEquals(1f, updated?.windows?.single()?.usagePercent)
        assertEquals(true, updated?.liveRead)
        assertEquals(CursorUsageMapper.GROK_BOT_PLAN_VIA_CURSOR, updated?.planLine)
    }

    @Test
    fun detachClearsGrokBotSnapshot() {
        FakeAccountStore.addLive(
            platformId = Platforms.CURSOR,
            work = false,
            planLine = "Pro+ · Cursor and other models counted apart",
            footNote = CursorUsageMapper.FOOTNOTE,
            windows = sampleWindows(),
        )
        FakeAccountStore.rememberGrokBotFromCursor(sampleGrokBotFromCursor())
        FakeAccountStore.detach()
        FakeAccountStore.addLive(
            platformId = Platforms.CURSOR,
            work = false,
            planLine = "Pro+ · Cursor and other models counted apart",
            footNote = CursorUsageMapper.FOOTNOTE,
            windows = sampleWindows(),
        )
        val bot = FakeAccountStore.addGrokBotFromCursor(work = false)
        requireNotNull(bot)
        assertEquals(0f, bot.windows.single().usagePercent)
    }

    @Test
    fun persistSurvivesDetachAndAttach() {
        val dir = kotlin.io.path.createTempDirectory("aisum-accounts").toFile()
        try {
            FakeAccountStore.attach(dir)
            val added = FakeAccountStore.addLive(
                platformId = Platforms.CLAUDE,
                work = false,
                planLine = "Max 5× · rolling session and weekly caps",
                footNote = "Session windows are five hours long and start with your first message.",
                windows = sampleWindows(),
            )
            FakeAccountStore.add(Platforms.GROK, work = false)
            FakeAccountStore.detach()
            assertTrue(FakeAccountStore.accounts.value.isEmpty())
            FakeAccountStore.attach(dir)
            assertEquals(2, FakeAccountStore.accounts.value.size)
            val restored = FakeAccountStore.account(added.id)
            assertEquals(Platforms.CLAUDE, restored?.platformId)
            assertEquals(true, restored?.liveRead)
            assertEquals(12f, restored?.windows?.first()?.usagePercent)
            assertEquals(false, FakeAccountStore.hasLive(Platforms.GROK))
            assertEquals(Platforms.GROK, FakeAccountStore.accountFor(Platforms.GROK)?.platformId)
        } finally {
            FakeAccountStore.detach()
            dir.deleteRecursively()
        }
    }

    @Test
    fun attachDropsSavedGemini() {
        val dir = kotlin.io.path.createTempDirectory("aisum-drop-gemini").toFile()
        try {
            FakeAccountStore.attach(dir)
            FakeAccountStore.add(Platforms.GEMINI, work = false)
            FakeAccountStore.add(Platforms.CLAUDE, work = false)
            FakeAccountStore.detach()
            FakeAccountStore.attach(dir)
            assertEquals(1, FakeAccountStore.accounts.value.size)
            assertEquals(Platforms.CLAUDE, FakeAccountStore.accounts.value.single().platformId)
        } finally {
            FakeAccountStore.detach()
            dir.deleteRecursively()
        }
    }

    @Test
    fun attachDropsSavedDeferredPlatforms() {
        val dir = kotlin.io.path.createTempDirectory("aisum-drop-deferred").toFile()
        try {
            FakeAccountStore.attach(dir)
            FakeAccountStore.add(Platforms.PERPLEXITY, work = false)
            FakeAccountStore.add(Platforms.COPILOT, work = false)
            FakeAccountStore.add(Platforms.M365, work = true)
            FakeAccountStore.add(Platforms.CLAUDE, work = false)
            FakeAccountStore.detach()
            FakeAccountStore.attach(dir)
            assertEquals(1, FakeAccountStore.accounts.value.size)
            assertEquals(Platforms.CLAUDE, FakeAccountStore.accounts.value.single().platformId)
        } finally {
            FakeAccountStore.detach()
            dir.deleteRecursively()
        }
    }

    private fun sampleWindows(): List<UsageWindow> = listOf(
        UsageWindow(
            name = "Current session",
            usagePercent = 12f,
            elapsedPercent = 20f,
            resetLabel = "Resets in 4 hr",
            elapsedLabel = "Window 20% elapsed",
        ),
    )

    private fun sampleGrokWindow(): List<UsageWindow> = listOf(
        UsageWindow(
            name = "Weekly SuperGrok Limit",
            usagePercent = 12f,
            elapsedPercent = 20f,
            resetLabel = "Resets in 4 hr",
            elapsedLabel = "Window 20% elapsed",
            segments = listOf(
                UsageSegment("Coding", 12f, 0xFF8C8783),
            ),
        ),
    )

    private fun sampleGrokBotFromCursor(): UsageReadResult.GrokBotUsage =
        UsageReadResult.GrokBotUsage(
            planLine = CursorUsageMapper.GROK_BOT_PLAN_VIA_CURSOR,
            windows = listOf(
                UsageWindow(
                    name = CursorUsageMapper.GROK_BOT_WINDOW,
                    usagePercent = 1f,
                    elapsedPercent = 43f,
                    resetLabel = "Resets Sun 12:00 AM",
                    elapsedLabel = "Window 43% elapsed",
                ),
            ),
        )
}
