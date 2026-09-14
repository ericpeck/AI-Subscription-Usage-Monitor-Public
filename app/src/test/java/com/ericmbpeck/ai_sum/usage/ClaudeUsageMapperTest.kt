package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ClaudeUsageMapperTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private val utc = ZoneOffset.UTC

    @Test
    fun mapsFiveHourAndSevenDayOnto4aWindows() {
        val resetsSession = Instant.parse("2026-09-04T16:29:00Z")
        val resetsWeekly = Instant.parse("2026-09-08T14:00:00Z")
        val raw = """
            {
              "kind": "ok",
              "subscription": { "plan": "Max 5x" },
              "usage": {
                "five_hour": { "utilization": 45, "resets_at": "$resetsSession" },
                "seven_day": { "utilization": 31, "resets_at": "$resetsWeekly" },
                "seven_day_opus": { "utilization": 90, "resets_at": "$resetsWeekly" }
              }
            }
        """.trimIndent()
        val result = ClaudeUsageMapper.map(raw, now, utc)
        val success = result as UsageReadResult.Success
        assertEquals("Max 5× · rolling session and weekly caps", success.planLine)
        assertEquals(2, success.windows.size)
        assertEquals(ClaudeUsageMapper.SESSION_NAME, success.windows[0].name)
        assertEquals(45f, success.windows[0].usagePercent, 0.01f)
        assertEquals(10.33f, success.windows[0].elapsedPercent, 0.05f)
        assertEquals("Resets in 4 hr 29 min", success.windows[0].resetLabel)
        assertEquals("Window 10% elapsed", success.windows[0].elapsedLabel)
        assertEquals(ClaudeUsageMapper.WEEKLY_NAME, success.windows[1].name)
        assertEquals(31f, success.windows[1].usagePercent, 0.01f)
        assertEquals("Resets Tue 2:00 PM", success.windows[1].resetLabel)
    }

    @Test
    fun weeklyOnlyStillShowsCurrentSession() {
        val raw = """
            {
              "kind": "ok",
              "subscription": { "plan": "Max 5x" },
              "usage": {
                "seven_day": { "utilization": 31, "resets_at": "2026-09-08T14:00:00Z" }
              }
            }
        """.trimIndent()
        val success = ClaudeUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals(2, success.windows.size)
        assertEquals(ClaudeUsageMapper.SESSION_NAME, success.windows[0].name)
        assertEquals(0f, success.windows[0].usagePercent, 0.01f)
        assertEquals("Resets in 5 hr", success.windows[0].resetLabel)
        assertEquals("Window 0% elapsed", success.windows[0].elapsedLabel)
        assertEquals(ClaudeUsageMapper.WEEKLY_NAME, success.windows[1].name)
        assertEquals(31f, success.windows[1].usagePercent, 0.01f)
    }

    @Test
    fun sessionWithoutResetsAtStillShowsUtilization() {
        val raw = """
            {
              "kind": "ok",
              "usage": {
                "five_hour": { "utilization": 12 },
                "seven_day": { "utilization": 31, "resets_at": "2026-09-08T14:00:00Z" }
              }
            }
        """.trimIndent()
        val success = ClaudeUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals(ClaudeUsageMapper.SESSION_NAME, success.windows[0].name)
        assertEquals(12f, success.windows[0].usagePercent, 0.01f)
        assertEquals("Resets in 5 hr", success.windows[0].resetLabel)
    }

    @Test
    fun freePlanKindIsRefused() {
        val result = ClaudeUsageMapper.map("""{"kind":"free"}""", now, utc)
        assertEquals(UsageReadResult.FreePlan, result)
    }

    @Test
    fun freePlanNameIsRefused() {
        val raw = """
            {
              "kind": "ok",
              "subscription": { "plan": "free" },
              "usage": {}
            }
        """.trimIndent()
        assertEquals(UsageReadResult.FreePlan, ClaudeUsageMapper.map(raw, now, utc))
    }

    @Test
    fun proPlanFromOrgRateLimitTierWhenSubscriptionHasNoLabel() {
        val raw = """
            {
              "kind": "ok",
              "subscription": { "status": "active", "billing_interval": "monthly" },
              "organization": { "uuid": "org-1", "name": "Personal", "rate_limit_tier": "default_claude_ai_pro" },
              "usage": {
                "five_hour": { "utilization": 12, "resets_at": "2026-09-04T16:29:00Z" },
                "seven_day": { "utilization": 8, "resets_at": "2026-09-08T14:00:00Z" }
              }
            }
        """.trimIndent()
        val success = ClaudeUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals("Pro · rolling session and weekly caps", success.planLine)
        assertEquals("Pro", ClaudeUsageMapper.displayPlan("default_claude_ai_pro"))
        assertEquals("Pro", ClaudeUsageMapper.displayPlan("claude_pro"))
        assertEquals("Max 5×", ClaudeUsageMapper.displayPlan("Max 5x"))
        assertEquals("Max 20×", ClaudeUsageMapper.displayPlan("default_claude_max_20x"))
    }

    @Test
    fun missingSessionReturnsNeedsSignIn() {
        assertEquals(
            UsageReadResult.NeedsSignIn,
            ClaudeUsageMapper.map("""{"kind":"needs_sign_in","status":403}""", now, utc),
        )
        assertEquals(
            UsageReadResult.NeedsSignIn,
            ClaudeUsageMapper.map("""{"kind":"wrong_origin","host":"accounts.google.com"}""", now, utc),
        )
    }

    @Test
    fun skipsOpusOnlyPayloadAsFree() {
        val raw = """
            {
              "kind": "ok",
              "usage": {
                "seven_day_opus": { "utilization": 12, "resets_at": "2026-09-08T14:00:00Z" }
              }
            }
        """.trimIndent()
        assertEquals(UsageReadResult.FreePlan, ClaudeUsageMapper.map(raw, now, utc))
    }
}

class ClaudeFetchScriptTest {
    @Test
    fun readsPlanFromOrgWhenSubscriptionHasNoLabel() {
        assertTrue(FETCH_SCRIPT.contains("function firstOrg"))
        assertTrue(FETCH_SCRIPT.contains("rate_limit_tier"))
        assertTrue(FETCH_SCRIPT.contains("organization: orgObj"))
        assertTrue(FETCH_SCRIPT.contains("looksLikePlan"))
        assertFalse(FETCH_SCRIPT.contains("json.tier || json.name || ''"))
    }
}

class ResetFormatterTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private val utc = ZoneOffset.UTC

    @Test
    fun elapsedIsTenPercentWhenThirtyOneMinutesIntoFiveHours() {
        val resetsAt = Instant.parse("2026-09-04T16:29:00Z")
        val percent = ResetFormatter.elapsedPercent(resetsAt, now, ResetFormatter.FIVE_HOURS)
        assertEquals(10.33f, percent, 0.05f)
        assertEquals("Window 10% elapsed", ResetFormatter.elapsedLabel(percent))
    }

    @Test
    fun remainingOverWindowIsZeroElapsed() {
        val resetsAt = now.plus(ResetFormatter.FIVE_HOURS).plusSeconds(60)
        assertEquals(0f, ResetFormatter.elapsedPercent(resetsAt, now, ResetFormatter.FIVE_HOURS), 0.01f)
    }

    @Test
    fun pastResetIsFullyElapsed() {
        val resetsAt = now.minusSeconds(1)
        assertEquals(100f, ResetFormatter.elapsedPercent(resetsAt, now, ResetFormatter.FIVE_HOURS), 0.01f)
        assertEquals("Resets now", ResetFormatter.resetLabel(resetsAt, now, utc))
    }

    @Test
    fun weeklyResetUsesWeekdayClock() {
        val resetsAt = Instant.parse("2026-09-08T14:00:00Z")
        assertEquals("Resets Tue 2:00 PM", ResetFormatter.resetLabel(resetsAt, now, utc))
        assertTrue(ResetFormatter.elapsedPercent(resetsAt, now, ResetFormatter.SEVEN_DAYS) < 100f)
    }

    @Test
    fun lastReadPhraseStripsUpdatedPrefix() {
        assertEquals("just now", ResetFormatter.lastReadPhrase("updated just now"))
        assertEquals("2 min ago", ResetFormatter.lastReadPhrase("updated 2 min ago"))
        assertEquals("Wed 8:12 AM", ResetFormatter.lastReadPhrase("Wed 8:12 AM"))
    }
}
