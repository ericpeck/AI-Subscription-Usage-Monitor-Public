package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorHostsTest {
    @Test
    fun apexAndWwwAreCursor() {
        assertTrue(CursorHosts.isCursorHost("https://cursor.com/dashboard"))
        assertTrue(CursorHosts.isCursorHost("https://www.cursor.com/agents"))
        assertFalse(CursorHosts.isCursorHost("https://api2.cursor.sh/aiserver"))
        assertFalse(CursorHosts.isCursorHost("https://accounts.google.com/signin"))
        assertFalse(CursorHosts.isCursorHost(null))
    }

    @Test
    fun autoReadOnSpendingOrAfterOauth() {
        assertTrue(CursorHosts.hasSpendingPath("https://cursor.com/dashboard/spending"))
        assertTrue(CursorHosts.hasSpendingPath("https://cursor.com/dashboard/spending?tab=usage"))
        assertTrue(CursorHosts.shouldAttemptUsageRead("https://cursor.com/dashboard/spending"))
        assertFalse(CursorHosts.shouldAttemptUsageRead("https://cursor.com/dashboard"))
        assertFalse(CursorHosts.shouldAttemptUsageRead("https://cursor.com/agents"))
        assertFalse(CursorHosts.shouldAttemptUsageRead("https://cursor.com/login"))
        assertFalse(CursorHosts.shouldAttemptUsageRead(CursorWebViewReader.SIGN_IN_URL))
        assertTrue(CursorHosts.shouldAttemptUsageRead(CursorWebViewReader.SPENDING_URL))
        assertTrue(
            CursorHosts.shouldAttemptUsageRead(
                "https://cursor.com/agents",
                "https://accounts.google.com/signin",
            ),
        )
        assertTrue(
            CursorHosts.shouldAttemptUsageRead(
                "https://cursor.com/agents",
                "https://authenticator.cursor.sh/",
            ),
        )
        assertTrue(
            CursorHosts.shouldAttemptUsageRead(
                "https://www.cursor.com/agents",
                "https://github.com/login",
            ),
        )
        assertTrue(
            CursorHosts.shouldAttemptUsageRead(
                "https://cursor.com/dashboard",
                "https://accounts.google.com/signin",
            ),
        )
        assertFalse(
            CursorHosts.shouldAttemptUsageRead(
                "https://cursor.com/agents",
                "https://cursor.com/dashboard",
            ),
        )
    }

    @Test
    fun googleAndGithubAreOauthNotProduct() {
        assertTrue(CursorHosts.isOauthHost("https://accounts.google.com/signin"))
        assertTrue(CursorHosts.isOauthHost("https://github.com/login"))
        assertTrue(CursorHosts.isOauthHost("https://authenticator.cursor.sh/"))
        assertFalse(CursorHosts.isOauthHost("https://cursor.com/dashboard/spending"))
        assertFalse(CursorHosts.shouldAttemptUsageRead("https://accounts.google.com/signin"))
    }
}
