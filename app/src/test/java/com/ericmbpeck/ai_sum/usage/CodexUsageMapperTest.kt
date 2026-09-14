package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class CodexUsageMapperTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private val utc = ZoneOffset.UTC
    private val sessionReset = Instant.parse("2026-09-04T16:24:00Z")
    private val weeklyReset = Instant.parse("2026-09-10T16:00:00Z")

    @Test
    fun mapsWhamPrimaryAndSecondaryOnto4c() {
        val raw = """
            {
              "kind": "ok",
              "plan_type": "plus",
              "rate_limit": {
                "allowed": true,
                "limit_reached": false,
                "primary_window": {
                  "used_percent": 20,
                  "limit_window_seconds": 18000,
                  "reset_after_seconds": 15840,
                  "reset_at": ${sessionReset.epochSecond}
                },
                "secondary_window": {
                  "used_percent": 58,
                  "limit_window_seconds": 604800,
                  "reset_after_seconds": 532800,
                  "reset_at": ${weeklyReset.epochSecond}
                }
              }
            }
        """.trimIndent()
        val success = CodexUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals("Plus · rolling session and weekly caps", success.planLine)
        assertEquals(2, success.windows.size)
        assertEquals(CodexUsageMapper.SESSION_NAME, success.windows[0].name)
        assertEquals(20f, success.windows[0].usagePercent, 0.01f)
        assertEquals("Resets in 4 hr 24 min", success.windows[0].resetLabel)
        assertEquals(CodexUsageMapper.WEEKLY_NAME, success.windows[1].name)
        assertEquals(58f, success.windows[1].usagePercent, 0.01f)
        assertEquals("Resets Thu 4:00 PM", success.windows[1].resetLabel)
    }

    @Test
    fun weeklyOnlyStillShowsCurrentSession() {
        val raw = """
            {
              "kind": "ok",
              "plan_type": "plus",
              "rate_limit": {
                "primary_window": {
                  "used_percent": 58,
                  "limit_window_seconds": 604800,
                  "reset_after_seconds": 532800,
                  "reset_at": ${weeklyReset.epochSecond}
                }
              }
            }
        """.trimIndent()
        val success = CodexUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals(2, success.windows.size)
        assertEquals(CodexUsageMapper.SESSION_NAME, success.windows[0].name)
        assertEquals(0f, success.windows[0].usagePercent, 0.01f)
        assertEquals("Resets in 5 hr", success.windows[0].resetLabel)
        assertEquals(CodexUsageMapper.WEEKLY_NAME, success.windows[1].name)
        assertEquals(58f, success.windows[1].usagePercent, 0.01f)
    }

    @Test
    fun jsWindowsArrayMapsDirectly() {
        val raw = """
            {
              "kind": "ok",
              "plan": "Pro",
              "windows": [
                { "name": "Current session", "percent": 12, "resets_at": "$sessionReset" },
                { "name": "Weekly limit", "percent": 40, "resets_at": "$weeklyReset" }
              ]
            }
        """.trimIndent()
        val success = CodexUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals("Pro · rolling session and weekly caps", success.planLine)
        assertEquals(12f, success.windows[0].usagePercent, 0.01f)
        assertEquals(40f, success.windows[1].usagePercent, 0.01f)
    }

    @Test
    fun freeWithoutWindowsIsRefused() {
        val raw = """
            { "kind": "ok", "plan_type": "free" }
        """.trimIndent()
        assertEquals(UsageReadResult.FreePlan, CodexUsageMapper.map(raw, now, utc))
    }

    @Test
    fun freeWithWindowsIsKept() {
        val raw = """
            {
              "kind": "ok",
              "plan_type": "go",
              "windows": [
                { "name": "Current session", "percent": 5, "resets_at": "$sessionReset" }
              ]
            }
        """.trimIndent()
        val success = CodexUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals("Go · rolling session and weekly caps", success.planLine)
        assertEquals(5f, success.windows[0].usagePercent, 0.01f)
        assertEquals(CodexUsageMapper.WEEKLY_NAME, success.windows[1].name)
        assertEquals(0f, success.windows[1].usagePercent, 0.01f)
    }

    @Test
    fun missingSessionReturnsNeedsSignIn() {
        assertEquals(
            UsageReadResult.NeedsSignIn,
            CodexUsageMapper.map("""{"kind":"needs_sign_in"}""", now, utc),
        )
        assertEquals(
            UsageReadResult.NeedsSignIn,
            CodexUsageMapper.map("""{"kind":"wrong_origin","host":"accounts.google.com"}""", now, utc),
        )
    }

    @Test
    fun creditsAreNotAThirdWindow() {
        val raw = """
            {
              "kind": "ok",
              "plan_type": "plus",
              "rate_limit": {
                "primary_window": {
                  "used_percent": 20,
                  "limit_window_seconds": 18000,
                  "reset_at": ${sessionReset.epochSecond}
                },
                "secondary_window": {
                  "used_percent": 58,
                  "limit_window_seconds": 604800,
                  "reset_at": ${weeklyReset.epochSecond}
                }
              },
              "credits": { "balance": 12.5 }
            }
        """.trimIndent()
        val success = CodexUsageMapper.map(raw, now, utc) as UsageReadResult.Success
        assertEquals(2, success.windows.size)
        assertTrue(success.windows.none { it.name.contains("credit", ignoreCase = true) })
    }
}
