package com.ericmbpeck.ai_sum.wearbridge

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object WearResetCopy {
    private val timeFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dayTimeFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.US)
    private val shortDayFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.US)

    fun curveWhen(
        resetsAtEpochMs: Long?,
        nowEpochMs: Long,
        fallbackResetLabel: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        if (resetsAtEpochMs == null) {
            return fallbackResetLabel.removePrefix("Resets ").trim()
        }
        val local = Instant.ofEpochMilli(resetsAtEpochMs).atZone(zone)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val resetDay: LocalDate = local.toLocalDate()
        val time = local.format(timeFmt)
        return when (resetDay) {
            today -> "Today $time"
            today.plusDays(1) -> "Tomorrow $time"
            else -> local.format(dayTimeFmt)
        }
    }

    fun centreReset(
        remainingMs: Long?,
        resetLabel: String,
        nowEpochMs: Long = 0L,
        resetsAtEpochMs: Long? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val remaining = remainingMs ?: resetsAtEpochMs?.minus(nowEpochMs)
        if (remaining != null && remaining > 0L && remaining < Duration.ofHours(48).toMillis()) {
            val totalMinutes = remaining / 60_000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            return "Resets in $hours:${minutes.toString().padStart(2, '0')}"
        }
        if (remaining != null && remaining > 0L && resetsAtEpochMs != null) {
            val local = Instant.ofEpochMilli(resetsAtEpochMs).atZone(zone)
            return "Resets ${local.format(shortDayFmt)}"
        }
        return resetLabel.ifBlank { "" }
    }
}
