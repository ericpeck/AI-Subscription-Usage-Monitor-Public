package com.ericmbpeck.ai_sum.watch

import androidx.compose.ui.graphics.Color

data class WatchInk(
    val outer: Color,
    val inner: Color,
    val figure: Color,
    val segments: List<Color> = emptyList(),
)

object WatchInks {
    val track = Color(0xFF302D2B)
    val tick = Color(0xFFCFCBC6)
    val rule = Color(0xFF3A3634)
    val reset = Color(0xFFD6D2CD)
    val secondary = Color(0xFFB0ABA5)
    val muted = Color(0xFF948F89)
    val name = Color(0xFFE8E5E1)
    val workGold = Color(0xFFD9B34D)
    val cursorBreach = Color(0xFFFF5A4D)
    val cursorBreachFigure = Color(0xFFFF7A6E)
    val emptyButton = Color(0xFF7CC6DD)
    val emptyButtonInk = Color(0xFF0D1C22)

    fun of(platformId: String): WatchInk = when (platformId) {
        "claude" -> WatchInk(Color(0xFFE08A6B), Color(0xFFE8A78E), Color(0xFFE08A6B))
        "gemini" -> WatchInk(Color(0xFF8FB4F2), Color(0xFFA6C6F6), Color(0xFF8FB4F2))
        "codex" -> WatchInk(Color(0xFF4FC0A3), Color(0xFF6ED0B6), Color(0xFF4FC0A3))
        "grok" -> WatchInk(
            outer = Color(0xFFF0ECE7),
            inner = Color(0xFFF0ECE7),
            figure = Color(0xFFF0ECE7),
            segments = listOf(Color(0xFFF0ECE7), Color(0xFFB5B0AA), Color(0xFF7D7873)),
        )
        "perplexity" -> WatchInk(Color(0xFF6FB3BD), Color(0xFF8AC6CE), Color(0xFF6FB3BD))
        "cursor" -> WatchInk(Color(0xFFB8B3AD), Color(0xFFB8B3AD), Color(0xFFB8B3AD))
        "copilot" -> WatchInk(Color(0xFFA98AE0), Color(0xFFBDA3EC), Color(0xFFA98AE0))
        "m365" -> WatchInk(Color(0xFF7FB4E8), Color(0xFF9AC6F0), Color(0xFF7FB4E8))
        "grokbot" -> WatchInk(Color(0xFFF0ECE7), Color(0xFFF0ECE7), Color(0xFFF0ECE7))
        else -> WatchInk(Color(0xFFE8E5E1), Color(0xFFB0ABA5), Color(0xFFE8E5E1))
    }
}
