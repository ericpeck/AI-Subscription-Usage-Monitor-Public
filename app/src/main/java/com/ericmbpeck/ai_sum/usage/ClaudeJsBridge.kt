package com.ericmbpeck.ai_sum.usage

import android.webkit.JavascriptInterface

internal const val CLAUDE_BRIDGE_NAME = "AISUM_CLAUDE_READ"
internal const val USAGE_BRIDGE_NAME = "AISUM_USAGE_READ"

internal class ClaudeJsBridge {
    @Volatile
    var onPayload: ((String) -> Unit)? = null

    @JavascriptInterface
    fun onResult(json: String) {
        onPayload?.invoke(json)
    }
}
