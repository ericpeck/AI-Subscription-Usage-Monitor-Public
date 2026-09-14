package com.ericmbpeck.ai_sum.usage

/**
 * What to drop from the shared WebView jar after Remove. Live WebView
 * platforms can coexist, but Remove is log-out for **that** platform.
 */
enum class SessionForget {
    None,
    All,
    Platform,
    ;

    companion object {
        fun afterRemove(
            removedPlatformId: String,
            remainingHasLiveWeb: Boolean,
        ): SessionForget {
            if (!LiveSignIn.usesInAppBrowser(removedPlatformId)) return None
            if (!remainingHasLiveWeb) return All
            return Platform
        }
    }
}
