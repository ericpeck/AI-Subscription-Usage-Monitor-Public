package com.ericmbpeck.ai_sum.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageWindowTest {
    @Test
    fun cursorModelsAtNinetyFourIsBreached() {
        val window = UsageWindow(
            name = "Cursor Models",
            usagePercent = 94f,
            elapsedPercent = 96f,
            resetLabel = "Resets 3 Sep",
            elapsedLabel = "Window 96% elapsed",
        )
        assertTrue(window.isBreached)
        assertEquals(-2f, window.paceDelta, 0.01f)
        assertEquals(
            "Tracking the clock almost exactly. This window should just about last.",
            paceCopy(window.paceDelta),
        )
    }

    @Test
    fun otherModelsAtSixtyFourIsNotBreached() {
        val window = UsageWindow(
            name = "Other Models",
            usagePercent = 64f,
            elapsedPercent = 96f,
            resetLabel = "Resets 3 Sep",
            elapsedLabel = "Window 96% elapsed",
        )
        assertFalse(window.isBreached)
        assertEquals(-32f, window.paceDelta, 0.01f)
        assertEquals(
            "Behind the clock by 32 points. Plenty of room left in this window.",
            paceCopy(window.paceDelta),
        )
    }

    @Test
    fun paceCopyAheadOfClock() {
        assertEquals(
            "Ahead of the clock by 20 points — at this rate you run out before it resets.",
            paceCopy(20f),
        )
    }

    @Test
    fun ninetyIsBreachedThreshold() {
        val window = UsageWindow("x", 90f, 50f, "", "")
        assertTrue(window.isBreached)
        val under = UsageWindow("x", 89.9f, 50f, "", "")
        assertFalse(under.isBreached)
    }
}
