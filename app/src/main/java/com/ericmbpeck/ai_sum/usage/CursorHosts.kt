package com.ericmbpeck.ai_sum.usage

/**
 * URL helpers for the Cursor in-app browser. Pure Kotlin so unit tests do not
 * need Robolectric; do not log cookies or query strings.
 */
object CursorHosts {
    const val DISPLAY: String = "cursor.com"

    fun isCursorHost(url: String?): Boolean {
        val host = hostOf(url)
        return host == "cursor.com" || host == "www.cursor.com"
    }

    fun isOauthHost(url: String?): Boolean {
        val host = hostOf(url)
        if (host.isEmpty()) return false
        return host == "accounts.google.com" ||
            host == "appleid.apple.com" ||
            host == "github.com" ||
            host == "www.github.com" ||
            host.endsWith(".github.com") ||
            host == "gitlab.com" ||
            host == "www.gitlab.com" ||
            host == "authenticator.cursor.sh" ||
            host == "authenticator.cursor.com"
    }

    fun displayHost(url: String?): String {
        val host = hostOf(url)
        return host.ifBlank { DISPLAY }
    }

    fun isLoginOrLogout(url: String?): Boolean {
        val path = pathOf(url)
        return path.startsWith("/login") ||
            path.startsWith("/logout") ||
            path.startsWith("/sign-in") ||
            path.startsWith("/signin") ||
            path.startsWith("/auth")
    }

    /**
     * Auto-read on the official spending page, or when cursor.com appears
     * immediately after Google / GitHub / Apple / authenticator (that is
     * signed-in `/agents`, not the pre-login dashboard). Continue remains
     * the fallback.
     */
    fun hasSpendingPath(url: String?): Boolean {
        val path = pathOf(url).lowercase()
        return path == "/dashboard/spending" || path.startsWith("/dashboard/spending/")
    }

    fun shouldAttemptUsageRead(url: String?, previousUrl: String? = null): Boolean {
        if (!isCursorHost(url)) return false
        if (isLoginOrLogout(url)) return false
        if (hasSpendingPath(url)) return true
        return isOauthHost(previousUrl)
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
