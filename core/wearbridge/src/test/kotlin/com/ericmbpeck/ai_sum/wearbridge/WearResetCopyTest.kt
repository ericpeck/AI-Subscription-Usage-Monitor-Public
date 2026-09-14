package com.ericmbpeck.ai_sum.wearbridge

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WearResetCopyTest {
    private val zone: ZoneId = ZoneId.of("America/Los_Angeles")
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 9, 9, 15, 0, 0, 0, zone)

    @Test
    fun todayAndTomorrowClockCopy() {
        val today = now.plusHours(4).plusMinutes(29).toInstant().toEpochMilli()
        assertEquals(
            "Today 7:29 PM",
            WearResetCopy.curveWhen(today, now.toInstant().toEpochMilli(), "", zone),
        )
        val tomorrow = now.plusDays(1).withHour(0).withMinute(0).toInstant().toEpochMilli()
        assertEquals(
            "Tomorrow 12:00 AM",
            WearResetCopy.curveWhen(tomorrow, now.toInstant().toEpochMilli(), "", zone),
        )
    }

    @Test
    fun centreUsesHourMinuteUnderFortyEightHours() {
        val remaining = (4L * 60 + 29) * 60_000L
        assertEquals("Resets in 4:29", WearResetCopy.centreReset(remaining, "Resets in 4 hr"))
    }

    @Test
    fun fallbackStripsResetsPrefix() {
        assertEquals(
            "Thu 3:01 PM",
            WearResetCopy.curveWhen(null, now.toInstant().toEpochMilli(), "Resets Thu 3:01 PM"),
        )
    }
}
