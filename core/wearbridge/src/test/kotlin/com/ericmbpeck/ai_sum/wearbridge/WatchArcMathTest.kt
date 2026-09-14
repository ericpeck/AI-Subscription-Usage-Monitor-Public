package com.ericmbpeck.ai_sum.wearbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchArcMathTest {
    @Test
    fun sessionIsOuterArcAndWeeklyIsInner() {
        val session = window("Current session", 45f)
        val weekly = window("Weekly limit", 31f)
        val (outer, inner) = WatchArcMath.splitWindows(listOf(session, weekly))
        assertEquals(session, outer)
        assertEquals(weekly, inner)
        assertEquals(45, WatchArcMath.centrePercent(account(session, weekly)))
    }

    @Test
    fun singleWindowUsesOuterOnly() {
        val weekly = window("Weekly SuperGrok Limit", 44f)
        val (outer, inner) = WatchArcMath.splitWindows(listOf(weekly))
        assertEquals(weekly, outer)
        assertEquals(null, inner)
    }

    @Test
    fun sweepAndTickStartAtTwelve() {
        assertEquals(162f, WatchArcMath.sweepDegrees(45f), 0.01f)
        assertEquals(-90f, WatchArcMath.tickAngleDegrees(0f), 0.01f)
        assertEquals(270f, WatchArcMath.tickAngleDegrees(100f), 0.01f)
    }

    @Test
    fun curveLabelJoinsNameUsedAndWhen() {
        val label = WatchArcMath.curveLabel(
            window("Current session", 45f).copy(curveWhen = "Today 7:29 PM"),
        )
        assertEquals("Current session · 45% · Today 7:29 PM", label)
    }

    private fun window(name: String, used: Float): WearWindow = WearWindow(
        name = name,
        usagePercent = used,
        elapsedPercent = 10f,
        resetLabel = "Resets in 4 hr",
        curveWhen = "Today 7:29 PM",
    )

    private fun account(vararg windows: WearWindow): WearAccount = WearAccount(
        id = 1L,
        platformId = "claude",
        displayName = "Claude",
        kind = "PERSONAL",
        planLine = "Pro",
        windows = windows.toList(),
    )
}
