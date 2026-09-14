package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.Platforms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class GrokUsageMapperTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private val utc = ZoneOffset.UTC

    @Test
    fun mapsWeeklyPoolAndSegmentsOnto4d() {
        val resetsWeekly = Instant.parse("2026-09-08T07:00:00Z")
        val raw = """
            {
              "kind": "ok",
              "percent": 44,
              "resets_at": "$resetsWeekly",
              "plan": "SuperGrok",
              "products": [
                { "name": "Voice", "percent": 19 },
                { "name": "Chat", "percent": 5 },
                { "name": "Build", "percent": 20 }
              ]
            }
        """.trimIndent()
        val result = GrokUsageMapper.map(raw, now, utc)
        val success = result as UsageReadResult.Success
        assertEquals("SuperGrok · one weekly limit, split by use", success.planLine)
        val window = success.windows.single()
        assertEquals(GrokUsageMapper.WINDOW_NAME, window.name)
        assertEquals(44f, window.usagePercent, 0.01f)
        assertEquals("Resets Tue 7:00 AM", window.resetLabel)
        assertEquals(3, window.segments.size)
        assertEquals("Voice", window.segments[0].label)
        assertEquals("Chat", window.segments[1].label)
        assertEquals("Coding", window.segments[2].label)
        assertEquals(19f, window.segments[0].percent, 0.01f)
        assertEquals(20f, window.segments[2].percent, 0.01f)
        assertEquals(0xFF201E1D, window.segments[0].color)
        assertEquals(0xFF56524F, window.segments[1].color)
        assertEquals(0xFF8C8783, window.segments[2].color)
    }

    @Test
    fun zeroPercentUsedIsValidSuperGrok() {
        val raw = """
            {
              "kind": "ok",
              "percent": 0,
              "resets_at": "2026-09-08T07:00:00Z",
              "plan": "SuperGrok",
              "products": []
            }
        """.trimIndent()
        val result = GrokUsageMapper.map(raw, now, utc)
        val success = result as UsageReadResult.Success
        assertEquals(0f, success.windows.single().usagePercent, 0.01f)
        assertTrue(success.windows.single().segments.isEmpty())
    }

    @Test
    fun totalOnlyHasNoSegments() {
        val raw = """
            {
              "kind": "ok",
              "percent": 12,
              "resets_at": "2026-09-08T07:00:00Z",
              "plan": "Heavy"
            }
        """.trimIndent()
        val result = GrokUsageMapper.map(raw, now, utc)
        val success = result as UsageReadResult.Success
        assertEquals("Heavy · one weekly limit, split by use", success.planLine)
        assertTrue(success.windows.single().segments.isEmpty())
        assertEquals(12f, success.windows.single().usagePercent, 0.01f)
    }

    @Test
    fun freePlanKindIsRefused() {
        assertEquals(UsageReadResult.FreePlan, GrokUsageMapper.map("""{"kind":"free"}""", now, utc))
    }

    @Test
    fun freePlanNameIsRefused() {
        val raw = """
            {
              "kind": "ok",
              "percent": 0,
              "plan": "free"
            }
        """.trimIndent()
        assertEquals(UsageReadResult.FreePlan, GrokUsageMapper.map(raw, now, utc))
    }

    @Test
    fun missingSessionReturnsNeedsSignIn() {
        assertEquals(
            UsageReadResult.NeedsSignIn,
            GrokUsageMapper.map("""{"kind":"needs_sign_in","status":403}""", now, utc),
        )
        assertEquals(
            UsageReadResult.NeedsSignIn,
            GrokUsageMapper.map("""{"kind":"wrong_origin","host":"x.com"}""", now, utc),
        )
    }

    @Test
    fun extraProductsKeepBoardColorsCycled() {
        val raw = """
            {
              "kind": "ok",
              "percent": 30,
              "resets_at": "2026-09-08T07:00:00Z",
              "plan": "SuperGrok",
              "products": [
                { "name": "Imagine", "percent": 10 },
                { "name": "API", "percent": 20 }
              ]
            }
        """.trimIndent()
        val window = (GrokUsageMapper.map(raw, now, utc) as UsageReadResult.Success).windows.single()
        assertEquals("API", window.segments[0].label)
        assertEquals("Imagine", window.segments[1].label)
        assertEquals(30f, window.usagePercent, 0.01f)
    }

    @Test
    fun xLoginSheetUsesProductSumWhenFirstPercentIsAProduct() {
        val pacific = ZoneId.of("America/Los_Angeles")
        val raw = """
            {
              "kind": "ok",
              "percent": 3,
              "resets_at": "September 10, 2026 at 3:01 PM",
              "plan": "SuperGrok",
              "products": [
                { "name": "Voice", "percent": 3 },
                { "name": "Chat", "percent": 2 }
              ]
            }
        """.trimIndent()
        val window = (GrokUsageMapper.map(raw, now, pacific) as UsageReadResult.Success).windows.single()
        assertEquals(5f, window.usagePercent, 0.01f)
        assertEquals("Voice", window.segments[0].label)
        assertEquals(3f, window.segments[0].percent, 0.01f)
        assertEquals("Chat", window.segments[1].label)
        assertEquals(2f, window.segments[1].percent, 0.01f)
        assertEquals("Resets Thu 3:01 PM", window.resetLabel)
        assertTrue(window.elapsedPercent > 0f)
    }

    @Test
    fun parsesGrokUsageSheetResetSentence() {
        val pacific = ZoneId.of("America/Los_Angeles")
        val expected = Instant.parse("2026-09-10T22:01:00Z")
        assertEquals(expected, GrokUsageMapper.parseReset("September 10, 2026 at 3:01 PM", pacific))
        assertEquals(
            expected,
            GrokUsageMapper.parseReset("Resets September 10, 2026 at 3:01 PM", pacific),
        )
        assertEquals(
            Instant.parse("2026-09-08T07:00:00Z"),
            GrokUsageMapper.parseReset("2026-09-08T07:00:00Z", pacific),
        )
    }
}

class GrokFetchScriptTest {
    @Test
    fun fetchLoopKeepsGoingAfter401ThenReadsDom() {
        assertTrue(GROK_FETCH_SCRIPT.contains("sawAuthFail"))
        assertTrue(GROK_FETCH_SCRIPT.contains("%\\s*used"))
        assertTrue(GROK_FETCH_SCRIPT.contains("Resets\\s+"))
        assertTrue(GROK_FETCH_SCRIPT.contains("what should we explore"))
        assertFalse(
            GROK_FETCH_SCRIPT.contains("send({ kind: 'needs_sign_in', status: res.status })"),
        )
    }

    @Test
    fun waitsForUsageSheetCopyBeforeFetch() {
        assertTrue(USAGE_VISIBLE_SCRIPT.contains("Weekly SuperGrok Limit"))
        assertTrue(USAGE_VISIBLE_SCRIPT.contains("%\\s*used"))
    }
}

class LiveSignInTest {
    @Test
    fun inAppBrowserIsClaudeGrokCursorAndCodex() {
        assertTrue(LiveSignIn.usesInAppBrowser(Platforms.CLAUDE))
        assertTrue(LiveSignIn.usesInAppBrowser(Platforms.GROK))
        assertTrue(LiveSignIn.usesInAppBrowser(Platforms.CURSOR))
        assertTrue(LiveSignIn.usesInAppBrowser(Platforms.CODEX))
        assertFalse(LiveSignIn.usesInAppBrowser(Platforms.GROK_BOT))
        assertFalse(LiveSignIn.usesInAppBrowser(Platforms.PERPLEXITY))
        assertEquals(
            LiveSignIn.reader(Platforms.CURSOR).hostLabel,
            LiveSignIn.reader(Platforms.GROK_BOT).hostLabel,
        )
        assertEquals(
            "https://cursor.com/dashboard/spending",
            LiveSignIn.reader(Platforms.GROK_BOT).readUrl,
        )
        assertEquals(
            CursorUsageMapper.GROK_BOT_FOOTNOTE,
            LiveSignIn.footnote(Platforms.GROK_BOT),
        )
        assertFalse(LiveSignIn.concealProductHost(Platforms.GROK))
        assertFalse(LiveSignIn.concealProductHost(Platforms.CURSOR))
        assertFalse(LiveSignIn.concealProductHost(Platforms.CODEX))
        assertTrue(LiveSignIn.concealProductHost(Platforms.CLAUDE))
        assertEquals("grok.com", LiveSignIn.reader(Platforms.GROK).hostLabel)
        assertEquals("cursor.com", LiveSignIn.reader(Platforms.CURSOR).hostLabel)
        assertEquals("chatgpt.com", LiveSignIn.reader(Platforms.CODEX).hostLabel)
        assertEquals("https://cursor.com/dashboard", LiveSignIn.reader(Platforms.CURSOR).signInUrl)
        assertEquals("https://cursor.com/dashboard/spending", LiveSignIn.reader(Platforms.CURSOR).readUrl)
        assertEquals("https://chatgpt.com/auth/login", LiveSignIn.reader(Platforms.CODEX).signInUrl)
        assertEquals("https://chatgpt.com/#settings/Usage", LiveSignIn.reader(Platforms.CODEX).readUrl)
        assertEquals(
            CodexUsageMapper.FOOTNOTE,
            LiveSignIn.footnote(Platforms.CODEX),
        )
        assertFalse(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CODEX,
                "https://chatgpt.com/",
            ),
        )
        assertTrue(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CODEX,
                "https://chatgpt.com/#settings/Usage",
            ),
        )
        assertTrue(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CODEX,
                "https://chatgpt.com/",
                "https://chatgpt.com/auth/login",
            ),
        )
        assertFalse(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CURSOR,
                "https://cursor.com/agents",
            ),
        )
        assertTrue(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CURSOR,
                "https://cursor.com/agents",
                "https://accounts.google.com/signin",
            ),
        )
        assertTrue(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.CURSOR,
                "https://cursor.com/dashboard/spending",
            ),
        )
        assertEquals("https://accounts.x.ai/sign-in?redirect=grok-com&return_to=%2F%3Fq%3D%26reasoningMode%3Dnone%26voice%3Dfalse", LiveSignIn.reader(Platforms.GROK).signInUrl)
        assertEquals("https://grok.com/?_s=usage", LiveSignIn.reader(Platforms.GROK).readUrl)
        assertFalse(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.GROK,
                LiveSignIn.reader(Platforms.GROK).signInUrl,
            ),
        )
        assertTrue(
            LiveSignIn.shouldAttemptUsageRead(
                Platforms.GROK,
                "https://grok.com/",
                "https://x.com/i/flow/login",
            ),
        )
    }
}
