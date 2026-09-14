package com.ericmbpeck.ai_sum.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexHostsTest {
    @Test
    fun chatgptAndChatOpenaiAreCodex() {
        assertTrue(CodexHosts.isChatGptHost("https://chatgpt.com/auth/login"))
        assertTrue(CodexHosts.isChatGptHost("https://www.chatgpt.com/"))
        assertTrue(CodexHosts.isChatGptHost("https://chat.openai.com/"))
        assertFalse(CodexHosts.isChatGptHost("https://accounts.google.com/signin"))
        assertFalse(CodexHosts.isChatGptHost("https://auth.openai.com/authorize"))
        assertFalse(CodexHosts.isChatGptHost(null))
    }

    @Test
    fun usageHashAndCodexSettingsAreTheReadSurface() {
        assertTrue(CodexHosts.hasUsageSurface("https://chatgpt.com/#settings/Usage"))
        assertTrue(CodexHosts.hasUsageSurface("https://chatgpt.com/#settings/usage"))
        assertTrue(CodexHosts.hasUsageSurface("https://chatgpt.com/#settings/Usage?foo=1"))
        assertTrue(CodexHosts.hasUsageSurface("https://chatgpt.com/codex/settings/usage"))
        assertFalse(CodexHosts.hasUsageSurface("https://chatgpt.com/"))
        assertFalse(CodexHosts.hasUsageSurface("https://chatgpt.com/c/abc"))
        assertFalse(CodexHosts.hasUsageSurface("https://chatgpt.com/auth/login"))
    }

    @Test
    fun autoReadOnUsageHashOrAfterLogin() {
        assertTrue(CodexHosts.shouldAttemptUsageRead("https://chatgpt.com/#settings/Usage"))
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                CodexWebViewReader.USAGE_URL,
            ),
        )
        assertFalse(CodexHosts.shouldAttemptUsageRead(CodexWebViewReader.SIGN_IN_URL))
        assertFalse(CodexHosts.shouldAttemptUsageRead("https://chatgpt.com/"))
        assertFalse(CodexHosts.shouldAttemptUsageRead("https://chatgpt.com/c/abc"))
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://accounts.google.com/signin",
            ),
        )
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://chatgpt.com/auth/login",
            ),
        )
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://auth.openai.com/authorize",
            ),
        )
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://appleid.apple.com/auth",
            ),
        )
        assertTrue(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://login.microsoftonline.com/common/oauth2",
            ),
        )
        assertFalse(
            CodexHosts.shouldAttemptUsageRead(
                "https://chatgpt.com/",
                "https://chatgpt.com/c/abc",
            ),
        )
        assertFalse(
            CodexHosts.shouldAttemptUsageRead(
                "https://accounts.google.com/signin",
            ),
        )
    }

    @Test
    fun googleAppleMicrosoftAreOauthNotProduct() {
        assertTrue(CodexHosts.isOauthHost("https://accounts.google.com/signin"))
        assertTrue(CodexHosts.isOauthHost("https://appleid.apple.com/auth"))
        assertTrue(CodexHosts.isOauthHost("https://login.microsoftonline.com/"))
        assertTrue(CodexHosts.isOauthHost("https://auth.openai.com/authorize"))
        assertFalse(CodexHosts.isOauthHost("https://chatgpt.com/"))
        assertFalse(CodexHosts.shouldAttemptUsageRead("https://accounts.google.com/signin"))
    }
}
