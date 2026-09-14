package com.ericmbpeck.ai_sum.wearbridge

/**
 * The board paints watch faces in a 454 CSS-pixel `.wv` circle. Those numbers are
 * **not** Android `sp` / `dp`. On a 454px xxhdpi (density 2) round they map to
 * half the raw value — 76px figure → 38sp — so the centre stack fits inside
 * the arcs instead of colliding with the curved labels.
 *
 * Type then takes [TYPE_MULTIPLIER] so the watch is a few sizes larger than
 * that strict map, without returning to raw `sp` (76sp on xxhdpi).
 */
object WatchBoardScale {
    const val FACE_PX = 454f
    const val SAFE_INSET_PX = 64f
    const val TYPE_MULTIPLIER = 1.15f

    fun dpValue(boardPx: Float, screenMinPx: Float, density: Float): Float {
        if (screenMinPx <= 0f || density <= 0f) return 0f
        return boardPx * screenMinPx / FACE_PX / density
    }

    fun spValue(boardPx: Float, screenMinPx: Float, density: Float): Float =
        dpValue(boardPx, screenMinPx, density) * TYPE_MULTIPLIER
}
