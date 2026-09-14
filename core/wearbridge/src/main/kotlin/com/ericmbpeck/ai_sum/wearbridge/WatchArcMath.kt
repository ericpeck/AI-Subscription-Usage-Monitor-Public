package com.ericmbpeck.ai_sum.wearbridge

object WatchArcMath {
    const val VIEWBOX = 380f
    const val CENTER = 190f
    const val OUTER_RADIUS = 168f
    const val OUTER_STROKE = 12f
    const val INNER_RADIUS = 132f
    const val INNER_STROKE = 9f
    const val OUTER_LABEL_RADIUS = 151.5f
    const val INNER_LABEL_RADIUS = 113f
    const val START_ANGLE_DEGREES = -90f

    fun usageFraction(percent: Float): Float = (percent / 100f).coerceIn(0f, 1f)

    fun sweepDegrees(usagePercent: Float): Float = usageFraction(usagePercent) * 360f

    fun tickAngleDegrees(elapsedPercent: Float): Float =
        START_ANGLE_DEGREES + usageFraction(elapsedPercent) * 360f

    fun splitWindows(windows: List<WearWindow>): Pair<WearWindow?, WearWindow?> {
        val outer = windows.getOrNull(0)
        val inner = windows.getOrNull(1)
        return outer to inner
    }

    fun centreWindow(account: WearAccount): WearWindow? {
        val (outer, inner) = splitWindows(account.windows)
        return outer ?: inner
    }

    fun centrePercent(account: WearAccount): Int =
        centreWindow(account)?.usagePercent?.toInt() ?: 0

    fun curveLabel(window: WearWindow): String {
        val used = "${window.usagePercent.toInt()}%"
        val whenLabel = window.curveWhen.ifBlank {
            window.resetLabel.removePrefix("Resets ").trim()
        }
        return listOf(window.name, used, whenLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
}
