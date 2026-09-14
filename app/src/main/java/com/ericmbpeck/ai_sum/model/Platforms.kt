package com.ericmbpeck.ai_sum.model

object Platforms {
    const val CLAUDE = "claude"
    const val GEMINI = "gemini"
    const val CODEX = "codex"
    const val GROK = "grok"
    const val PERPLEXITY = "perplexity"
    const val CURSOR = "cursor"
    const val COPILOT = "copilot"
    const val M365 = "m365"
    const val GROK_BOT = "grokbot"

    val catalog: List<Platform> = listOf(
        Platform(CLAUDE, "Claude", "Claude", 0xFFD97757, 0xFFB8552F, "Pro · Max 5× · Max 20×", true),
        Platform(CODEX, "Codex", "Codex", 0xFF10A37F, 0xFF0B7D61, "Plus · Pro", true),
        Platform(GROK, "Grok", "Grok", 0xFF201E1D, 0xFF201E1D, "SuperGrok · Heavy", true),
        Platform(GROK_BOT, "Grok Bot", "Grok Bot", 0xFF201E1D, 0xFF201E1D, "Included with SuperGrok", true),
        Platform(PERPLEXITY, "Perplexity", "Perplexity", 0xFF20808D, 0xFF196A75, "Pro · Max", false),
        Platform(CURSOR, "Cursor", "Cursor", 0xFF7D7979, 0xFF4F4C4C, "Pro · Ultra", true),
        Platform(COPILOT, "GitHub Copilot", "Copilot", 0xFF6E40C9, 0xFF5A2FA8, "Pro · Pro+", false),
        Platform(GEMINI, "Gemini", "Gemini", 0xFF4285F4, 0xFF2F66C9, "AI Pro · AI Ultra", false),
        Platform(M365, "Microsoft 365 Copilot", "M365", 0xFF0F6CBD, 0xFF0B5699, "Personal · Business", false),
    )

    fun byId(id: String): Platform = catalog.first { it.id == id }
}
