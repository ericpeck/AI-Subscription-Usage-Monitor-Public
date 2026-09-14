package com.ericmbpeck.ai_sum.usage

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object ResetFormatter {
    val FIVE_HOURS: Duration = Duration.ofHours(5)
    val SEVEN_DAYS: Duration = Duration.ofDays(7)
    val THIRTY_DAYS: Duration = Duration.ofDays(30)

    private val weekdayTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE h:mm a", Locale.US)

    fun resetLabel(resetsAt: Instant, now: Instant, zone: ZoneId): String {
        val remaining = Duration.between(now, resetsAt)
        if (remaining.isNegative || remaining.isZero) {
            return "Resets now"
        }
        if (remaining < Duration.ofHours(24)) {
            val hours = remaining.toHours()
            val minutes = remaining.minusHours(hours).toMinutes()
            return when {
                hours <= 0L -> "Resets in $minutes min"
                minutes <= 0L -> "Resets in $hours hr"
                else -> "Resets in $hours hr $minutes min"
            }
        }
        val local = resetsAt.atZone(zone)
        return "Resets ${local.format(weekdayTime)}"
    }

    fun elapsedPercent(resetsAt: Instant, now: Instant, window: Duration): Float {
        val remaining = Duration.between(now, resetsAt)
        if (remaining.isNegative || remaining.isZero) {
            return 100f
        }
        if (remaining >= window) {
            return 0f
        }
        val elapsedMillis = window.minus(remaining).toMillis().toFloat()
        return ((elapsedMillis / window.toMillis()) * 100f).coerceIn(0f, 100f)
    }

    fun elapsedLabel(percent: Float): String {
        val n = percent.roundToInt().coerceIn(0, 100)
        return "Window $n% elapsed"
    }

    fun updatedJustNow(): String = "updated just now"

    fun lastReadPhrase(updatedLabel: String): String {
        val trimmed = updatedLabel.trim()
        return if (trimmed.startsWith("updated ")) {
            trimmed.removePrefix("updated ").ifBlank { trimmed }
        } else {
            trimmed
        }
    }
}
