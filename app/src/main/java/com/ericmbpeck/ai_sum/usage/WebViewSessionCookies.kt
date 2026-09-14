package com.ericmbpeck.ai_sum.usage

import android.webkit.CookieManager
import android.webkit.WebStorage
import com.ericmbpeck.ai_sum.model.Platforms

/**
 * CookieManager is one jar for Claude, SuperGrok, Cursor, and Codex. Remove must still log
 * that platform out: expire its hosts and delete its WebStorage, and leave
 * the other platform's cookies alone. Never log cookie names or values.
 */
object WebViewSessionCookies {
    val grokOrigins: List<String> = listOf(
        "https://grok.com",
        "https://www.grok.com",
        "https://grok.x.ai",
        "https://accounts.x.ai",
        "https://auth.x.ai",
        "https://x.ai",
        "https://www.x.ai",
        "https://x.com",
        "https://www.x.com",
        "https://mobile.x.com",
        "https://api.x.com",
        "https://twitter.com",
        "https://www.twitter.com",
        "https://mobile.twitter.com",
        "https://api.twitter.com",
        "https://abs.twimg.com",
    )

    val claudeOrigins: List<String> = listOf(
        "https://claude.ai",
        "https://www.claude.ai",
    )

    val cursorOrigins: List<String> = listOf(
        "https://cursor.com",
        "https://www.cursor.com",
        "https://authenticator.cursor.sh",
        "https://authenticator.cursor.com",
    )

    val codexOrigins: List<String> = listOf(
        "https://chatgpt.com",
        "https://www.chatgpt.com",
        "https://chat.openai.com",
        "https://auth.openai.com",
    )

    fun originsFor(platformId: String): List<String> = when (platformId) {
        Platforms.GROK -> grokOrigins
        Platforms.CLAUDE -> claudeOrigins
        Platforms.CURSOR -> cursorOrigins
        Platforms.CODEX -> codexOrigins
        else -> emptyList()
    }

    fun forgetAll() {
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().flush()
    }

    fun forget(platformId: String) {
        val origins = originsFor(platformId)
        if (origins.isEmpty()) return
        val cookies = CookieManager.getInstance()
        for (origin in origins) {
            expireCookies(cookies, origin)
            runCatching { WebStorage.getInstance().deleteOrigin(origin) }
        }
        cookies.flush()
    }

    private fun expireCookies(cookies: CookieManager, origin: String) {
        val header = cookies.getCookie(origin) ?: return
        val host = hostOfOrigin(origin)
        for (name in cookieNames(header)) {
            for (value in expiredSetCookieValues(name, host)) {
                cookies.setCookie(origin, value)
            }
        }
    }

    internal fun cookieNames(header: String?): List<String> {
        if (header.isNullOrBlank()) return emptyList()
        return header.split(';')
            .map { it.substringBefore('=').trim() }
            .filter { it.isNotEmpty() }
    }

    internal fun expiredSetCookieValues(name: String, host: String): List<String> {
        val expired = "$name=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT"
        val out = mutableListOf(
            "$expired; Path=/",
            "$expired; Path=/; Secure",
        )
        for (domain in domainCandidates(host)) {
            out += "$expired; Path=/; Domain=$domain"
            out += "$expired; Path=/; Domain=$domain; Secure"
        }
        return out
    }

    internal fun domainCandidates(host: String): List<String> {
        if (host.isEmpty()) return emptyList()
        val out = mutableListOf(host, ".$host")
        val parts = host.split('.')
        if (parts.size >= 2) {
            val parent = parts.takeLast(2).joinToString(".")
            if (parent != host) {
                out += parent
                out += ".$parent"
            }
        }
        return out.distinct()
    }

    internal fun hostOfOrigin(origin: String): String =
        origin.substringAfter("://").substringBefore('/').substringBefore(':').lowercase()
}
