package com.ericmbpeck.ai_sum.usage

import com.ericmbpeck.ai_sum.model.UsageWindow

sealed interface UsageReadResult {
    data class GrokBotUsage(
        val planLine: String,
        val windows: List<UsageWindow>,
    )

    data class Success(
        val planLine: String,
        val windows: List<UsageWindow>,
        val grokBot: GrokBotUsage? = null,
    ) : UsageReadResult

    data object FreePlan : UsageReadResult

    data object NeedsSignIn : UsageReadResult

    data class Failed(val reason: String) : UsageReadResult
}
