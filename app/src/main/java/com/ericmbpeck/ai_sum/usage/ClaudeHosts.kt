package com.ericmbpeck.ai_sum.usage

/**
 * URL helpers for the Claude in-app browser. Pure Kotlin so unit tests do not
 * need Robolectric; do not log cookies or query strings.
 */
object ClaudeHosts {
    const val DISPLAY: String = "claude.ai"

    fun isClaudeHost(url: String?): Boolean {
        val host = hostOf(url)
        return host == "claude.ai" || host == "www.claude.ai"
    }

    fun displayHost(url: String?): String {
        val host = hostOf(url)
        return host.ifBlank { DISPLAY }
    }

    fun isLoginOrLogout(url: String?): Boolean {
        val path = pathOf(url)
        return path.startsWith("/login") || path.startsWith("/logout")
    }

    /**
     * Auto-read once Claude is past `/login` — including signed-in `/` ("Afternoon, …").
     * Skip Google/Apple and the login/logout routes. Continue remains the fallback
     * if the fetch still returns [UsageReadResult.NeedsSignIn].
     */
    fun shouldAttemptUsageRead(url: String?): Boolean {
        if (!isClaudeHost(url)) return false
        return !isLoginOrLogout(url)
    }

    /**
     * Android WebView's default UA contains `; wv` and `Version/4.0`, which
     * Google's sign-in page treats as an embedded browser and refuses. Strip
     * those tokens so the page is the same Chrome the phone already has.
     */
    fun chromeLikeUserAgent(raw: String): String =
        raw.replace("; wv", "").replace("Version/4.0 ", "")

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
