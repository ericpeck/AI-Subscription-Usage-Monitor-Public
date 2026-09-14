package com.ericmbpeck.ai_sum.wearbridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchLabelClearanceTest {
    @Test
    fun bufferIsSixteenPxOnTheBoardFace() {
        assertEquals(16f, WatchLabelClearance.bufferPx(454f), 0.01f)
    }

    @Test
    fun innerLabelInwardRadiusIsInsideThePath() {
        val inward = WatchLabelClearance.labelInwardRadiusPx(454f, hasInnerLabel = true)
        val path = WatchArcMath.INNER_LABEL_RADIUS * 454f / WatchArcMath.VIEWBOX
        assertTrue(inward < path)
        assertTrue(inward > path * 0.8f)
    }

    @Test
    fun noLiftWhenResetAlreadyHasTheBuffer() {
        val cx = 227f
        val cy = 227f
        val r = 125f
        val lift = WatchLabelClearance.liftPx(
            resetLeft = 180f,
            resetRight = 274f,
            resetBottom = cy + r - 40f,
            cx = cx,
            cy = cy,
            labelInwardRadius = r,
            bufferPx = 16f,
        )
        assertEquals(0f, lift, 0.01f)
    }

    @Test
    fun liftsUntilBufferWhenResetSitsOnTheCurve() {
        val cx = 227f
        val cy = 227f
        val r = 125f
        val lift = WatchLabelClearance.liftPx(
            resetLeft = 226f,
            resetRight = 228f,
            resetBottom = cy + r - 5f,
            cx = cx,
            cy = cy,
            labelInwardRadius = r,
            bufferPx = 16f,
        )
        assertEquals(11f, lift, 0.05f)
    }

    @Test
    fun resetSlotOnlyDependsOnScreenSize() {
        val a = WatchLabelClearance.resetSlotTopFromCenterPx(454f)
        val b = WatchLabelClearance.resetSlotTopFromCenterPx(454f)
        assertEquals(a, b, 0f)
        assertTrue(a > 0f)
        assertTrue(WatchLabelClearance.resetSlotTopFromCenterPx(320f) > 0f)
    }

    @Test
    fun resetSlotClearsAWideDateOnTheInnerCurve() {
        val screen = 454f
        val cx = 227f
        val cy = 227f
        val top = WatchLabelClearance.resetSlotTopFromCenterPx(screen)
        val bottom = cy + top + WatchLabelClearance.resetLinePx(screen)
        val half = WatchLabelClearance.canonicalResetHalfWidthPx(screen)
        val lift = WatchLabelClearance.liftPx(
            resetLeft = cx - half,
            resetRight = cx + half,
            resetBottom = bottom,
            cx = cx,
            cy = cy,
            labelInwardRadius = WatchLabelClearance.labelInwardRadiusPx(
                screen,
                hasInnerLabel = true,
            ),
            bufferPx = WatchLabelClearance.bufferPx(screen),
        )
        assertEquals(0f, lift, 0.5f)
    }

    @Test
    fun windowRelativeLiftJumpsWhenThePagerOffsetsTheText() {
        val screen = 454f
        val cx = 227f
        val cy = 227f
        val r = WatchLabelClearance.labelInwardRadiusPx(screen, hasInnerLabel = true)
        val buf = WatchLabelClearance.bufferPx(screen)
        val bottom = cy + r - 5f
        val settled = WatchLabelClearance.liftPx(
            resetLeft = 226f,
            resetRight = 228f,
            resetBottom = bottom,
            cx = cx,
            cy = cy,
            labelInwardRadius = r,
            bufferPx = buf,
        )
        val sliding = WatchLabelClearance.liftPx(
            resetLeft = 306f,
            resetRight = 308f,
            resetBottom = bottom,
            cx = cx,
            cy = cy,
            labelInwardRadius = r,
            bufferPx = buf,
        )
        assertTrue(kotlin.math.abs(sliding - settled) > 5f)
    }

    @Test
    fun wideResetUsesTheCloserCornerNotTheSixOClockGap() {
        val cx = 227f
        val cy = 227f
        val r = 125f
        val resetBottom = 320f
        val lift = WatchLabelClearance.liftPx(
            resetLeft = 147f,
            resetRight = 307f,
            resetBottom = resetBottom,
            cx = cx,
            cy = cy,
            labelInwardRadius = r,
            bufferPx = 16f,
        )
        val dx = 80f
        val labelYAtCorner = cy + kotlin.math.sqrt(r * r - dx * dx)
        val expected = (16f - (labelYAtCorner - resetBottom)).coerceAtLeast(0f)
        assertEquals(expected, lift, 0.05f)
        assertTrue(lift > 0f)
    }
}
