package com.ericmbpeck.ai_sum.wearbridge

import kotlin.math.min
import kotlin.math.sqrt

/**
 * The reset line (and the Grok legend) sit in **one slot** on every face,
 * sized so a wide date still has [BUFFER_FACE_PX] of air above the inner
 * weekly curve. 16px on the 454 face is about one reset-line of empty.
 *
 * Do not re-measure that gap from `positionInRoot` while paging. The pager
 * moves the text in window space while the circle stays at the window centre,
 * so the old lift jumped the line into the usage figure mid-swipe.
 */
object WatchLabelClearance {
    const val BUFFER_FACE_PX = 16f
    const val LABEL_TEXT_VIEWBOX = 11f
    const val GLYPH_INWARD_FACTOR = 1.2f

    /** One reset line on the 454 face (14 board px type after the 1.15× bump). */
    const val RESET_LINE_FACE_PX = 16f

    /**
     * Half-width of a long centre reset (`Resets Fri 12:00 AM`) on the 454 face.
     * Corners of a wide line meet the inner curve before the six-o'clock gap.
     */
    const val CANONICAL_RESET_HALF_WIDTH_FACE_PX = 80f

    fun bufferPx(screenMinPx: Float): Float =
        BUFFER_FACE_PX * screenMinPx / WatchBoardScale.FACE_PX

    fun resetLinePx(screenMinPx: Float): Float =
        RESET_LINE_FACE_PX * screenMinPx / WatchBoardScale.FACE_PX

    fun canonicalResetHalfWidthPx(screenMinPx: Float): Float =
        CANONICAL_RESET_HALF_WIDTH_FACE_PX * screenMinPx / WatchBoardScale.FACE_PX

    /**
     * Distance from the face centre down to the **top** of the reset slot.
     * Inner curve, canonical wide line, every platform — so the Y does not
     * change when the pager swaps Claude for Cursor or Grok Bot.
     */
    fun resetSlotTopFromCenterPx(screenMinPx: Float): Float {
        val inward = labelInwardRadiusPx(screenMinPx, hasInnerLabel = true)
        val dx = min(canonicalResetHalfWidthPx(screenMinPx), inward * 0.95f)
        val labelYFromCenter = sqrt(inward * inward - dx * dx)
        return labelYFromCenter - bufferPx(screenMinPx) - resetLinePx(screenMinPx)
    }

    fun labelInwardRadiusPx(screenMinPx: Float, hasInnerLabel: Boolean): Float {
        val viewScale = screenMinPx / WatchArcMath.VIEWBOX
        val pathR = (if (hasInnerLabel) {
            WatchArcMath.INNER_LABEL_RADIUS
        } else {
            WatchArcMath.OUTER_LABEL_RADIUS
        }) * viewScale
        val glyphInward = LABEL_TEXT_VIEWBOX * viewScale * GLYPH_INWARD_FACTOR
        return (pathR - glyphInward).coerceAtLeast(0f)
    }

    fun liftPx(
        resetLeft: Float,
        resetRight: Float,
        resetBottom: Float,
        cx: Float,
        cy: Float,
        labelInwardRadius: Float,
        bufferPx: Float,
        samples: Int = 24,
    ): Float {
        if (resetRight <= resetLeft || labelInwardRadius <= 0f || samples <= 0) return 0f
        var minGap = Float.POSITIVE_INFINITY
        for (i in 0..samples) {
            val t = i / samples.toFloat()
            val x = resetLeft + (resetRight - resetLeft) * t
            val dx = x - cx
            val r2 = labelInwardRadius * labelInwardRadius
            if (dx * dx >= r2) continue
            val labelY = cy + sqrt(r2 - dx * dx)
            val gap = labelY - resetBottom
            minGap = min(minGap, gap)
        }
        if (minGap == Float.POSITIVE_INFINITY) return 0f
        return (bufferPx - minGap).coerceAtLeast(0f)
    }
}
