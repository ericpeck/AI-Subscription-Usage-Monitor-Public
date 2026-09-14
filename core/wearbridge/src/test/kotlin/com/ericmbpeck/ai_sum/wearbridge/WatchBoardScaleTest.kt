package com.ericmbpeck.ai_sum.wearbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchBoardScaleTest {
    @Test
    fun boardPixelsAreNotRawSpOnXxhdpiRound() {
        val figureDp = WatchBoardScale.dpValue(
            boardPx = 76f,
            screenMinPx = 454f,
            density = 2f,
        )
        assertEquals(38f, figureDp, 0.01f)
        val nameDp = WatchBoardScale.dpValue(23f, 454f, 2f)
        assertEquals(11.5f, nameDp, 0.01f)
        val safeDp = WatchBoardScale.dpValue(WatchBoardScale.SAFE_INSET_PX, 454f, 2f)
        assertEquals(32f, safeDp, 0.01f)
    }

    @Test
    fun typeIsAFewSizesLargerThanStrictBoardMap() {
        val figureSp = WatchBoardScale.spValue(
            boardPx = 76f,
            screenMinPx = 454f,
            density = 2f,
        )
        assertEquals(38f * WatchBoardScale.TYPE_MULTIPLIER, figureSp, 0.01f)
        assertEquals(43.7f, figureSp, 0.01f)
    }
}
