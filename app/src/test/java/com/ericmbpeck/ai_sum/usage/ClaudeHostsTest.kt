package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudeHostsTest {
    @Test
    fun apexAndWwwAreClaude() {
        assertTrue(ClaudeHosts.isClaudeHost("https://claude.ai/login"))
        assertTrue(ClaudeHosts.isClaudeHost("https://www.claude.ai/new"))
        assertFalse(ClaudeHosts.isClaudeHost("https://accounts.google.com/signin"))
        assertFalse(ClaudeHosts.isClaudeHost("about:blank"))
        assertFalse(ClaudeHosts.isClaudeHost(null))
    }

    @Test
    fun displayHostFallsBackToClaude() {
        assertEquals("accounts.google.com", ClaudeHosts.displayHost("https://accounts.google.com/o/oauth2"))
        assertEquals("claude.ai", ClaudeHosts.displayHost(null))
        assertEquals("claude.ai", ClaudeHosts.displayHost("about:blank"))
    }

    @Test
    fun autoReadSkipsLoginAndGoogleButNotSignedInHome() {
        assertFalse(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai/login"))
        assertFalse(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai/login?returnTo=/new"))
        assertFalse(ClaudeHosts.shouldAttemptUsageRead("https://accounts.google.com/signin"))
        assertTrue(ClaudeHosts.isLoginOrLogout("https://claude.ai/logout"))
        assertTrue(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai/"))
        assertTrue(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai"))
        assertTrue(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai/new"))
        assertTrue(ClaudeHosts.shouldAttemptUsageRead("https://claude.ai/chat/abc"))
        assertTrue(ClaudeHosts.shouldAttemptUsageRead("https://www.claude.ai/recents"))
    }

    @Test
    fun chromeLikeUserAgentDropsWebViewTokens() {
        val webViewUa =
            "Mozilla/5.0 (Linux; Android 16; Pixel 8 Build/AP2A; wv) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Version/4.0 Chrome/131.0.6778.135 Mobile Safari/537.36"
        val chrome = ClaudeHosts.chromeLikeUserAgent(webViewUa)
        assertFalse(chrome.contains("; wv"))
        assertFalse(chrome.contains("Version/4.0"))
        assertTrue(chrome.contains("Chrome/131.0.6778.135"))
    }
}
