package com.ericmbpeck.ai_sum.watch

import com.ericmbpeck.ai_sum.wearbridge.WearAccount
import com.ericmbpeck.ai_sum.wearbridge.WearSegment
import com.ericmbpeck.ai_sum.wearbridge.WearSnapshot
import com.ericmbpeck.ai_sum.wearbridge.WearWindow

object SampleWearSnapshots {
    fun boardClaude(): WearSnapshot = WearSnapshot(
        updatedEpochMs = 1L,
        accounts = listOf(
            WearAccount(
                id = 1L,
                platformId = "claude",
                displayName = "Claude",
                kind = "PERSONAL",
                planLine = "Pro",
                windows = listOf(
                    WearWindow(
                        name = "Current session",
                        usagePercent = 45f,
                        elapsedPercent = 20f,
                        resetLabel = "Resets in 4:29",
                        curveWhen = "Today 7:29 PM",
                        remainingMs = (4L * 60 + 29) * 60_000L,
                    ),
                    WearWindow(
                        name = "Weekly limit",
                        usagePercent = 31f,
                        elapsedPercent = 40f,
                        resetLabel = "Resets Tue 1 Sep, 7:00 AM",
                        curveWhen = "Tue 1 Sep, 7:00 AM",
                    ),
                ),
            ),
        ),
    )

    fun grok(): WearSnapshot = WearSnapshot(
        updatedEpochMs = 1L,
        accounts = listOf(
            WearAccount(
                id = 2L,
                platformId = "grok",
                displayName = "Grok",
                kind = "PERSONAL",
                planLine = "SuperGrok",
                windows = listOf(
                    WearWindow(
                        name = "Weekly SuperGrok Limit",
                        usagePercent = 44f,
                        elapsedPercent = 10f,
                        resetLabel = "Resets Tomorrow 12:00 AM",
                        curveWhen = "Tomorrow 12:00 AM",
                        segments = listOf(
                            WearSegment("Voice", 19f),
                            WearSegment("Chat", 5f),
                            WearSegment("Coding", 20f),
                        ),
                    ),
                ),
            ),
        ),
    )
}
