package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.Platforms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionForgetTest {
    @Test
    fun removingGrokWhileClaudeIsLiveDropsOnlyThatPlatform() {
        assertEquals(
            SessionForget.Platform,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.GROK,
                remainingHasLiveWeb = true,
            ),
        )
    }

    @Test
    fun removingCursorWhileClaudeIsLiveDropsOnlyCursor() {
        assertEquals(
            SessionForget.Platform,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.CURSOR,
                remainingHasLiveWeb = true,
            ),
        )
    }

    @Test
    fun removingLastLiveWebViewTabDropsTheWholeJar() {
        assertEquals(
            SessionForget.All,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.GROK,
                remainingHasLiveWeb = false,
            ),
        )
        assertEquals(
            SessionForget.All,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.CURSOR,
                remainingHasLiveWeb = false,
            ),
        )
    }

    @Test
    fun fakePlatformsDoNotTouchTheJar() {
        assertEquals(
            SessionForget.None,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.PERPLEXITY,
                remainingHasLiveWeb = true,
            ),
        )
        assertEquals(
            SessionForget.None,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.GROK_BOT,
                remainingHasLiveWeb = true,
            ),
        )
        assertEquals(
            SessionForget.None,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.GROK_BOT,
                remainingHasLiveWeb = false,
            ),
        )
    }

    @Test
    fun removingCodexWhileClaudeIsLiveDropsOnlyCodex() {
        assertEquals(
            SessionForget.Platform,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.CODEX,
                remainingHasLiveWeb = true,
            ),
        )
        assertEquals(
            SessionForget.All,
            SessionForget.afterRemove(
                removedPlatformId = Platforms.CODEX,
                remainingHasLiveWeb = false,
            ),
        )
    }
}

class WebViewSessionCookiesTest {
    @Test
    fun grokOriginsCoverXLoginAndNotClaudeOrGoogle() {
        assertTrue(WebViewSessionCookies.grokOrigins.contains("https://grok.com"))
        assertTrue(WebViewSessionCookies.grokOrigins.contains("https://accounts.x.ai"))
        assertTrue(WebViewSessionCookies.grokOrigins.contains("https://x.com"))
        assertFalse(WebViewSessionCookies.grokOrigins.any { it.contains("claude") })
        assertFalse(WebViewSessionCookies.grokOrigins.any { it.contains("google") })
        assertFalse(WebViewSessionCookies.grokOrigins.any { it.contains("apple") })
        assertEquals(
            WebViewSessionCookies.grokOrigins,
            WebViewSessionCookies.originsFor(Platforms.GROK),
        )
        assertEquals(
            WebViewSessionCookies.claudeOrigins,
            WebViewSessionCookies.originsFor(Platforms.CLAUDE),
        )
        assertEquals(
            WebViewSessionCookies.cursorOrigins,
            WebViewSessionCookies.originsFor(Platforms.CURSOR),
        )
        assertTrue(WebViewSessionCookies.cursorOrigins.contains("https://cursor.com"))
        assertFalse(WebViewSessionCookies.cursorOrigins.any { it.contains("google") })
        assertEquals(
            WebViewSessionCookies.codexOrigins,
            WebViewSessionCookies.originsFor(Platforms.CODEX),
        )
        assertTrue(WebViewSessionCookies.codexOrigins.contains("https://chatgpt.com"))
        assertTrue(WebViewSessionCookies.codexOrigins.contains("https://auth.openai.com"))
        assertFalse(WebViewSessionCookies.codexOrigins.any { it.contains("google") })
        assertTrue(WebViewSessionCookies.originsFor(Platforms.PERPLEXITY).isEmpty())
    }

    @Test
    fun cookieNamesIgnoreValues() {
        assertEquals(
            listOf("auth_token", "ct0"),
            WebViewSessionCookies.cookieNames("auth_token=secret; ct0=also-secret"),
        )
        assertTrue(WebViewSessionCookies.cookieNames(null).isEmpty())
    }

    @Test
    fun expiredCookiesIncludeParentDomain() {
        val values = WebViewSessionCookies.expiredSetCookieValues("auth_token", "www.x.com")
        assertTrue(values.any { it.contains("Domain=.x.com") })
        assertTrue(values.any { it.startsWith("auth_token=;") })
        assertFalse(values.any { it.contains("secret") })
    }
}
