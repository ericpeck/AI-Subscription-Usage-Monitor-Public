package com.ericmbpeck.ai_sum.wear

import com.ericmbpeck.ai_sum.data.fakeClaudeAccount
import com.ericmbpeck.ai_sum.model.AccountKind
import com.ericmbpeck.ai_sum.model.Platforms
import com.ericmbpeck.ai_sum.model.UsageWindow
import com.ericmbpeck.ai_sum.wearbridge.WatchArcMath
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSnapshotMapperTest {
    private val zone: ZoneId = ZoneId.of("America/Los_Angeles")
    private val now: Instant = ZonedDateTime.of(2026, 9, 9, 15, 0, 0, 0, zone).toInstant()

    @Test
    fun claudeSessionStaysOnTheOuterArc() {
        val resets = now.plusSeconds(4 * 3600 + 29 * 60).toEpochMilli()
        val account = fakeClaudeAccount(1, AccountKind.PERSONAL).copy(
            windows = listOf(
                UsageWindow(
                    name = "Current session",
                    usagePercent = 45f,
                    elapsedPercent = 20f,
                    resetLabel = "Resets in 4 hr",
                    elapsedLabel = "Window 20% elapsed",
                    resetsAtEpochMs = resets,
                ),
                UsageWindow(
                    name = "Weekly limit",
                    usagePercent = 31f,
                    elapsedPercent = 40f,
                    resetLabel = "Resets Tue 7:00 AM",
                    elapsedLabel = "Window 40% elapsed",
                ),
            ),
        )
        val wear = WearSnapshotMapper.from(listOf(account), now, zone).accounts.single()
        val (outer, inner) = WatchArcMath.splitWindows(wear.windows)
        assertEquals("Current session", outer?.name)
        assertEquals("Weekly limit", inner?.name)
        assertEquals(45, WatchArcMath.centrePercent(wear))
        assertEquals("Today 7:29 PM", outer?.curveWhen)
        assertTrue(wear.viaLine.isEmpty())
    }

    @Test
    fun grokBotViaCursor() {
        val account = fakeClaudeAccount(2, AccountKind.PERSONAL).copy(
            platformId = Platforms.GROK_BOT,
            planLine = "Included with Cursor",
        )
        val wear = WearSnapshotMapper.from(listOf(account), now, zone).accounts.single()
        assertEquals("Grok Bot", wear.displayName)
        assertEquals("via Cursor", wear.viaLine)
    }
}
