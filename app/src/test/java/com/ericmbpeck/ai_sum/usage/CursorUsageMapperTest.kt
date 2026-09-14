package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class CursorUsageMapperTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private val utc = ZoneOffset.UTC

    @Test
    fun mapsTwoSpendingPoolsOnto4f() {
        val resets = Instant.parse("2026-10-03T07:00:00Z")
        val raw = """
            {
              "kind": "ok",
              "plan": "Pro",
              "windows": [
                { "name": "Included", "percent": 94, "resets_at": "$resets" },
                { "name": "API", "percent": 64, "resets_at": "$resets" }
              ]
            }
        """.trimIndent()
        val result = CursorUsageMapper.map(raw, now, utc)
        val success = result as UsageReadResult.Success
        assertEquals("Pro · Cursor and other models counted apart", success.planLine)
        assertEquals(2, success.windows.size)
        assertEquals(CursorUsageMapper.CURSOR_MODELS, success.windows[0].name)
        assertEquals(94f, success.windows[0].usagePercent, 0.01f)
        assertEquals(CursorUsageMapper.OTHER_MODELS, success.windows[1].name)
        assertEquals(64f, success.windows[1].usagePercent, 0.01f)
        assertEquals("Resets Sat 7:00 AM", success.windows[0].resetLabel)
        assertTrue(success.windows[0].elapsedPercent > 0f)
    }

    @Test
    fun singlePercentIsOneFillNotASecondWindow() {
        val raw = """
            {
              "kind": "ok",
              "percent": 40,
              "resets_at": "2026-10-03T07:00:00Z",
              "plan": "Ultra",
              "windows": []
            }
        """.trimIndent()
        val windows = (CursorUsageMapper.map(raw, now, utc) as UsageReadResult.Success).windows
        assertEquals(1, windows.size)
        assertEquals("Included usage", windows.single().name)
        assertEquals(40f, windows.single().usagePercent, 0.01f)
    }

    @Test
    fun hobbyPlanIsRefused() {
        val raw = """
            { "kind": "ok", "percent": 0, "plan": "hobby" }
        """.trimIndent()
        assertEquals(UsageReadResult.FreePlan, CursorUsageMapper.map(raw, now, utc))
    }

    @Test
    fun missingSessionReturnsNeedsSignIn() {
        assertEquals(
            UsageReadResult.NeedsSignIn,
            CursorUsageMapper.map("""{"kind":"needs_sign_in"}""", now, utc),
        )
        assertEquals(
            UsageReadResult.NeedsSignIn,
            CursorUsageMapper.map("""{"kind":"wrong_origin","host":"accounts.google.com"}""", now, utc),
        )
    }

    @Test
    fun spendingUpgradeCardDoesNotBecomeThePlan() {
        val spending = """
            CURRENT PLAN
            Pro+ ${'$'}60/mo
            Usage limits reset on Sep 18 (9 days left)
            UPGRADE AVAILABLE
            Ultra ${'$'}200/mo
            Cursor Models 27% used
            Other Models 12% used
        """.trimIndent()
        assertEquals("Pro+", CursorUsageMapper.planFromSpendingText(spending))
        assertEquals("Pro+", CursorUsageMapper.displayPlan("Pro+"))
        val mapped = CursorUsageMapper.map(
            """{"kind":"ok","plan":"Ultra","percent":27,"windows":[]}""",
            now,
            utc,
        ) as UsageReadResult.Success
        assertEquals("Ultra · Cursor and other models counted apart", mapped.planLine)
        val proPlus = CursorUsageMapper.map(
            """{"kind":"ok","plan":"Pro+","percent":27,"windows":[]}""",
            now,
            utc,
        ) as UsageReadResult.Success
        assertEquals("Pro+ · Cursor and other models counted apart", proPlus.planLine)
    }

    @Test
    fun parsesEnglishResetDate() {
        val instant = CursorUsageMapper.parseReset("October 3, 2026", utc)
        assertEquals(Instant.parse("2026-10-03T00:00:00Z"), instant)
        val withTime = CursorUsageMapper.parseReset("Resets October 3, 2026 at 7:00 AM", utc)
        assertEquals(Instant.parse("2026-10-03T07:00:00Z"), withTime)
    }

    @Test
    fun grokBotWeeklyIsNotACursorWindow() {
        val exhibitNow = Instant.parse("2026-09-09T16:00:00Z")
        val raw = """
            {
              "kind": "ok",
              "plan": "Pro+",
              "windows": [
                { "name": "Cursor Models", "percent": 27, "resets_at": "Sep 18" },
                { "name": "Other Models", "percent": 12, "resets_at": "Sep 18" },
                { "name": "Grok Bot", "percent": 1, "resets_at": "Sep 13" }
              ],
              "grok_bot": { "percent": 1, "resets_at": "Sep 13" }
            }
        """.trimIndent()
        val success = CursorUsageMapper.map(raw, exhibitNow, utc) as UsageReadResult.Success
        assertEquals("Pro+ · Cursor and other models counted apart", success.planLine)
        assertEquals(2, success.windows.size)
        assertEquals(CursorUsageMapper.CURSOR_MODELS, success.windows[0].name)
        assertEquals(27f, success.windows[0].usagePercent, 0.01f)
        assertEquals(CursorUsageMapper.OTHER_MODELS, success.windows[1].name)
        assertEquals(12f, success.windows[1].usagePercent, 0.01f)
        assertTrue(success.windows.none { it.name.contains("Grok", ignoreCase = true) })
        val grokBot = success.grokBot
        requireNotNull(grokBot)
        assertEquals(CursorUsageMapper.GROK_BOT_PLAN_VIA_CURSOR, grokBot.planLine)
        val weekly = grokBot.windows.single()
        assertEquals(CursorUsageMapper.GROK_BOT_WINDOW, weekly.name)
        assertEquals(1f, weekly.usagePercent, 0.01f)
        assertTrue(weekly.elapsedPercent > 0f)
        assertTrue(weekly.elapsedPercent < 100f)
        assertTrue(weekly.resetLabel.isNotBlank())
    }

    @Test
    fun parseResetMonthDayFromSpendingCopy() {
        val exhibitNow = Instant.parse("2026-09-09T16:00:00Z")
        assertEquals(
            Instant.parse("2026-09-13T00:00:00Z"),
            CursorUsageMapper.parseReset("Sep 13", utc, exhibitNow),
        )
        assertEquals(
            Instant.parse("2026-09-13T00:00:00Z"),
            CursorUsageMapper.parseReset("Resets Sep 13 (4 days left)", utc, exhibitNow),
        )
        assertEquals(
            Instant.parse("2026-09-18T00:00:00Z"),
            CursorUsageMapper.parseReset("Usage limits reset on Sep 18 (9 days left)", utc, exhibitNow),
        )
        val afterReset = Instant.parse("2026-09-14T00:00:00Z")
        assertEquals(
            Instant.parse("2027-09-13T00:00:00Z"),
            CursorUsageMapper.parseReset("Sep 13", utc, afterReset),
        )
    }
}

class CursorFetchScriptTest {
    @Test
    fun neverCallsDashboardServiceRpc() {
        assertTrue(CURSOR_FETCH_SCRIPT.contains("api2.cursor.sh"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("if (url.indexOf('api2.cursor.sh') !== -1) continue;"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("if (url.indexOf('aiserver') !== -1) continue;"))
        assertFalse(CURSOR_FETCH_SCRIPT.contains("method: 'POST'"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("CURRENT PLAN"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("function planFromText"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("function grokBotFromText"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("Weekly usage"))
        assertTrue(CURSOR_FETCH_SCRIPT.contains("Usage limits reset on"))
        assertTrue(CURSOR_SPENDING_VISIBLE_SCRIPT.contains("spending"))
    }
}
