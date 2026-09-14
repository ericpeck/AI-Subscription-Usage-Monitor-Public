package com.ericmbpeck.ai_sum.usage

import android.webkit.WebView

interface UsageReader {
    val signInUrl: String
    val readUrl: String
    val hostLabel: String

    suspend fun read(webView: WebView): UsageReadResult
}
