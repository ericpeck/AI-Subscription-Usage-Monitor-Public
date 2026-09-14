package com.ericmbpeck.ai_sum.wearbridge

import kotlinx.serialization.Serializable

object WearDataPaths {
    const val SNAPSHOT = "/aisum/snapshot"
    const val OPEN = "/aisum/open"
    const val OPEN_SCHEME = "aisum"
    const val OPEN_HOST = "open"
    const val OPEN_URI = "aisum://open"
    const val JSON_KEY = "json"
}

@Serializable
data class WearSnapshot(
    val updatedEpochMs: Long,
    val accounts: List<WearAccount> = emptyList(),
)

@Serializable
data class WearAccount(
    val id: Long,
    val platformId: String,
    val displayName: String,
    val kind: String,
    val planLine: String,
    val viaLine: String = "",
    val needsReauth: Boolean = false,
    val windows: List<WearWindow> = emptyList(),
)

@Serializable
data class WearWindow(
    val name: String,
    val usagePercent: Float,
    val elapsedPercent: Float,
    val resetLabel: String,
    val curveWhen: String,
    val remainingMs: Long? = null,
    val segments: List<WearSegment> = emptyList(),
)

@Serializable
data class WearSegment(
    val label: String,
    val percent: Float,
)
