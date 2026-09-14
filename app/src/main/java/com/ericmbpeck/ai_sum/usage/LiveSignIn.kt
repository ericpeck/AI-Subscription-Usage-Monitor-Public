package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.Platforms

object LiveSignIn {
    fun usesInAppBrowser(platformId: String): Boolean =
        platformId == Platforms.CLAUDE ||
            platformId == Platforms.GROK ||
            platformId == Platforms.CURSOR ||
            platformId == Platforms.CODEX

    fun reader(platformId: String): UsageReader = when (platformId) {
        Platforms.CLAUDE -> ClaudeWebViewReader
        Platforms.GROK -> GrokWebViewReader
        Platforms.CURSOR,
        Platforms.GROK_BOT,
        -> CursorWebViewReader
        Platforms.CODEX -> CodexWebViewReader
        else -> error("No live reader for $platformId")
    }

    fun footnote(platformId: String): String = when (platformId) {
        Platforms.GROK -> GrokUsageMapper.FOOTNOTE
        Platforms.CLAUDE -> ClaudeUsageMapper.FOOTNOTE
        Platforms.CURSOR -> CursorUsageMapper.FOOTNOTE
        Platforms.GROK_BOT -> CursorUsageMapper.GROK_BOT_FOOTNOTE
        Platforms.CODEX -> CodexUsageMapper.FOOTNOTE
        else -> error("No live footnote for $platformId")
    }

    fun shouldAttemptUsageRead(
        platformId: String,
        url: String?,
        previousUrl: String? = null,
    ): Boolean = when (platformId) {
        Platforms.CLAUDE -> ClaudeHosts.shouldAttemptUsageRead(url)
        Platforms.GROK -> GrokHosts.shouldAttemptUsageRead(url, previousUrl)
        Platforms.CURSOR,
        Platforms.GROK_BOT,
        -> CursorHosts.shouldAttemptUsageRead(url, previousUrl)
        Platforms.CODEX -> CodexHosts.shouldAttemptUsageRead(url, previousUrl)
        else -> false
    }

    fun isProductHost(platformId: String, url: String?): Boolean = when (platformId) {
        Platforms.CLAUDE -> ClaudeHosts.isClaudeHost(url)
        Platforms.GROK -> GrokHosts.isGrokHost(url)
        Platforms.CURSOR,
        Platforms.GROK_BOT,
        -> CursorHosts.isCursorHost(url)
        Platforms.CODEX -> CodexHosts.isChatGptHost(url)
        else -> false
    }

    fun isAnyProductHost(url: String?): Boolean =
        ClaudeHosts.isClaudeHost(url) ||
            GrokHosts.isGrokHost(url) ||
            CursorHosts.isCursorHost(url) ||
            CodexHosts.isChatGptHost(url)

    fun displayHost(platformId: String, url: String?): String = when (platformId) {
        Platforms.GROK -> GrokHosts.displayHost(url)
        Platforms.CURSOR,
        Platforms.GROK_BOT,
        -> CursorHosts.displayHost(url)
        Platforms.CODEX -> CodexHosts.displayHost(url)
        else -> ClaudeHosts.displayHost(url)
    }

    fun hostLabel(platformId: String): String = when {
        usesInAppBrowser(platformId) -> reader(platformId).hostLabel
        else -> Platforms.byId(platformId).displayName
    }

    /**
     * Claude's signed-in home is not the product UI — hide it. SuperGrok,
     * Cursor, and Codex only conceal while reading.
     */
    fun concealProductHost(platformId: String): Boolean = platformId == Platforms.CLAUDE

    fun persistCookies() {
        ClaudeWebViewReader.persistCookies()
    }

    fun forgetSession() {
        WebViewSessionCookies.forgetAll()
    }

    fun forgetPlatform(platformId: String) {
        WebViewSessionCookies.forget(platformId)
    }

    fun forgetAfterRemove(
        removedPlatformId: String,
        remainingHasLiveWeb: Boolean,
    ) {
        when (SessionForget.afterRemove(removedPlatformId, remainingHasLiveWeb)) {
            SessionForget.None -> Unit
            SessionForget.All -> WebViewSessionCookies.forgetAll()
            SessionForget.Platform -> WebViewSessionCookies.forget(removedPlatformId)
        }
    }
}
