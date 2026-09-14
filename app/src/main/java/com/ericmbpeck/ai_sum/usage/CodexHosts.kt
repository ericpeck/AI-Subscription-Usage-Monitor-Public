package com.ericmbpeck.ai_sum.usage

/**
 * URL helpers for the Codex in-app browser. Pure Kotlin so unit tests do not
 * need Robolectric; do not log cookies or query strings.
 */
object CodexHosts {
    const val DISPLAY: String = "chatgpt.com"

    fun isChatGptHost(url: String?): Boolean {
        val host = hostOf(url)
        return host == "chatgpt.com" ||
            host == "www.chatgpt.com" ||
            host == "chat.openai.com"
    }

    fun isOauthHost(url: String?): Boolean {
        val host = hostOf(url)
        if (host.isEmpty()) return false
        return host == "accounts.google.com" ||
            host == "appleid.apple.com" ||
            host == "login.microsoftonline.com" ||
            host == "login.live.com" ||
            host == "auth.openai.com" ||
            host.endsWith(".microsoftonline.com")
    }

    fun displayHost(url: String?): String {
        val host = hostOf(url)
        return host.ifBlank { DISPLAY }
    }

    fun isLoginOrLogout(url: String?): Boolean {
        val path = pathOf(url)
        return path.startsWith("/auth") ||
            path.startsWith("/login") ||
            path.startsWith("/logout") ||
            path.startsWith("/sign-in") ||
            path.startsWith("/signin")
    }

    fun hasUsageSurface(url: String?): Boolean {
        val frag = fragmentOf(url)
        if (frag.contains("settings/usage")) return true
        val path = pathOf(url).lowercase()
        return path == "/codex/settings/usage" ||
            path.startsWith("/codex/settings/usage/")
    }

    /**
     * Auto-read on Settings → Usage, or when chatgpt.com appears immediately
     * after Google / Apple / Microsoft / auth.openai.com / ChatGPT login
     * (signed-in chat, not the login form). Continue remains the fallback.
     */
    fun shouldAttemptUsageRead(url: String?, previousUrl: String? = null): Boolean {
        if (!isChatGptHost(url)) return false
        if (isLoginOrLogout(url)) return false
        if (hasUsageSurface(url)) return true
        if (isOauthHost(previousUrl)) return true
        return isChatGptHost(previousUrl) && isLoginOrLogout(previousUrl)
    }

    internal fun fragmentOf(url: String?): String {
        val s = url.orEmpty().trim()
        if (s.isEmpty() || !s.contains('#')) return ""
        return s.substringAfter('#').substringBefore('?').lowercase()
    }

    internal fun hostOf(url: String?): String {
        val s = url.orEmpty().trim()
        if (s.isEmpty() || s.startsWith("about:")) return ""
        val noFrag = s.substringBefore('#')
        val noQuery = noFrag.substringBefore('?')
        val afterScheme = if (noQuery.contains("://")) {
            noQuery.substringAfter("://")
        } else {
            return ""
        }
        return afterScheme.substringBefore('/').substringBefore(':').lowercase()
    }

    internal fun pathOf(url: String?): String {
        val s = url.orEmpty().trim()
        if (s.isEmpty() || s.startsWith("about:")) return ""
        val noFrag = s.substringBefore('#')
        val noQuery = noFrag.substringBefore('?')
        if (!noQuery.contains("://")) return "/"
        val afterHost = noQuery.substringAfter("://").substringAfter('/', missingDelimiterValue = "")
        return if (afterHost.isEmpty()) "/" else "/$afterHost"
    }
}
