package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GrokHostsTest {
    @Test
    fun apexWwwAndGrokXAiAreGrok() {
        assertTrue(GrokHosts.isGrokHost("https://grok.com/?_s=usage"))
        assertTrue(GrokHosts.isGrokHost("https://www.grok.com/"))
        assertTrue(GrokHosts.isGrokHost("https://grok.x.ai/?_s=usage"))
        assertFalse(GrokHosts.isGrokHost("https://accounts.x.ai/signin"))
        assertFalse(GrokHosts.isGrokHost("https://x.com/i/flow/login"))
        assertFalse(GrokHosts.isGrokHost("about:blank"))
        assertFalse(GrokHosts.isGrokHost(null))
    }

    @Test
    fun displayHostFallsBackToGrok() {
        assertEquals("x.com", GrokHosts.displayHost("https://x.com/i/flow/login"))
        assertEquals("accounts.x.ai", GrokHosts.displayHost("https://accounts.x.ai/authorize"))
        assertEquals("grok.com", GrokHosts.displayHost(null))
        assertEquals("grok.com", GrokHosts.displayHost("about:blank"))
    }

    @Test
    fun oauthHostsAreNotProduct() {
        assertTrue(GrokHosts.isOauthHost("https://accounts.x.ai/signin"))
        assertTrue(GrokHosts.isOauthHost("https://x.com/i/flow/login"))
        assertTrue(GrokHosts.isOauthHost("https://www.twitter.com/login"))
        assertTrue(GrokHosts.isOauthHost("https://accounts.google.com/signin"))
        assertFalse(GrokHosts.isOauthHost("https://grok.com/?_s=usage"))
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://x.com/i/flow/login"))
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://accounts.x.ai/signin"))
        assertTrue(GrokHosts.isOauthHost(GrokWebViewReader.SIGN_IN_URL))
        assertFalse(GrokHosts.shouldAttemptUsageRead(GrokWebViewReader.SIGN_IN_URL))
    }

    @Test
    fun autoReadOnUsageQueryOrAfterOauth() {
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://grok.com/login"))
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://grok.com/sign-in"))
        assertTrue(GrokHosts.isLoginOrLogout("https://grok.com/logout"))
        assertTrue(GrokHosts.shouldAttemptUsageRead("https://grok.com/?_s=usage"))
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://grok.com/"))
        assertFalse(GrokHosts.shouldAttemptUsageRead("https://www.grok.com/chat"))
        assertTrue(
            GrokHosts.shouldAttemptUsageRead(
                "https://grok.com/",
                "https://accounts.x.ai/sign-in?redirect=grok-com",
            ),
        )
        assertTrue(
            GrokHosts.shouldAttemptUsageRead(
                "https://grok.com/",
                "https://x.com/i/flow/login",
            ),
        )
        assertTrue(
            GrokHosts.shouldAttemptUsageRead(
                "https://www.grok.com/chat",
                "https://accounts.google.com/signin",
            ),
        )
        assertFalse(
            GrokHosts.shouldAttemptUsageRead(
                "https://grok.com/",
                "https://grok.com/?foo=1",
            ),
        )
    }

    @Test
    fun usageQueryIsTheConfirmedDeepLink() {
        assertTrue(GrokHosts.hasUsageQuery("https://grok.com/?_s=usage"))
        assertTrue(GrokHosts.hasUsageQuery("https://grok.com/?foo=1&_s=usage#tab"))
        assertFalse(GrokHosts.hasUsageQuery("https://grok.com/"))
        assertFalse(GrokHosts.hasUsageQuery("https://grok.com/?_s=settings"))
        assertFalse(GrokHosts.hasUsageQuery(null))
    }
}
