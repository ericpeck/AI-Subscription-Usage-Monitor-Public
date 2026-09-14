package com.ericmbpeck.ai_sum.usage

/**
 * URL helpers for the SuperGrok in-app browser. Pure Kotlin so unit tests do not
 * need Robolectric; do not log cookies or query strings.
 */
object GrokHosts {
    const val DISPLAY: String = "grok.com"

    fun isGrokHost(url: String?): Boolean {
        val host = hostOf(url)
        return host == "grok.com" || host == "www.grok.com" || host == "grok.x.ai"
    }

    fun isOauthHost(url: String?): Boolean {
        val host = hostOf(url)
        if (host.isEmpty()) return false
        return host == "accounts.x.ai" ||
            host == "x.com" ||
            host == "www.x.com" ||
            host == "twitter.com" ||
            host == "www.twitter.com" ||
            host == "api.twitter.com" ||
            host.endsWith(".twitter.com") ||
            host.endsWith(".x.com") ||
            host == "accounts.google.com" ||
            host == "appleid.apple.com"
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

    fun hasUsageQuery(url: String?): Boolean {
        val query = url.orEmpty().substringAfter('?', missingDelimiterValue = "")
            .substringBefore('#')
        if (query.isEmpty()) return false
        return query.split('&').any { part ->
            val key = part.substringBefore('=')
            val value = part.substringAfter('=', missingDelimiterValue = "")
            key == "_s" && value == "usage"
        }
    }

    /**
     * Auto-read on the usage deep link, or when grok.com chat appears
     * immediately after X / Google / Apple / xAI (that is signed-in home,
     * not the pre-login chat). `/?_s=usage` before a session is still an
     * Account sheet — 1d must not start there. Continue remains the fallback.
     */
    fun shouldAttemptUsageRead(url: String?, previousUrl: String? = null): Boolean {
        if (!isGrokHost(url)) return false
        if (isLoginOrLogout(url)) return false
        if (hasUsageQuery(url)) return true
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
