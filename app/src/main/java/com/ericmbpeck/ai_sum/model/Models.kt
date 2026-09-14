package com.ericmbpeck.ai_sum.model

import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
enum class AccountKind {
    PERSONAL,
    WORK,
}

data class Platform(
    val id: String,
    val displayName: String,
    val tabLabel: String,
    val barColor: Long,
    val figureColor: Long,
    val seedPlans: String,
    val addableThisSlice: Boolean,
)

@Serializable
data class Account(
    val id: Long,
    val platformId: String,
    val kind: AccountKind,
    val planLine: String,
    val footNote: String,
    val updatedLabel: String,
    val windows: List<UsageWindow>,
    val liveRead: Boolean = false,
    val needsReauth: Boolean = false,
    val email: String = "",
    val accountDetailLine: String = "",
    val notifyOnReset: Boolean = true,
)

@Serializable
data class UsageSegment(
    val label: String,
    val percent: Float,
    val color: Long,
)

@Serializable
data class UsageWindow(
    val name: String,
    val usagePercent: Float,
    val elapsedPercent: Float,
    val resetLabel: String,
    val elapsedLabel: String,
    val segments: List<UsageSegment> = emptyList(),
    val resetsAtEpochMs: Long? = null,
) {
    val usageFraction: Float get() = (usagePercent / 100f).coerceIn(0f, 1f)
    val elapsedFraction: Float get() = (elapsedPercent / 100f).coerceIn(0f, 1f)
    val isBreached: Boolean get() = usagePercent >= ALERT_THRESHOLD
    val paceDelta: Float get() = usagePercent - elapsedPercent
}

const val ALERT_THRESHOLD = 90f
const val PACE_TOLERANCE = 12f

fun paceCopy(paceDelta: Float): String {
    val points = abs(paceDelta).toInt()
    return when {
        paceDelta > PACE_TOLERANCE ->
            "Ahead of the clock by $points points — at this rate you run out before it resets."
        paceDelta < -PACE_TOLERANCE ->
            "Behind the clock by $points points. Plenty of room left in this window."
        else ->
            "Tracking the clock almost exactly. This window should just about last."
    }
}
