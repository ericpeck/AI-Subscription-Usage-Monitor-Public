package com.ericmbpeck.ai_sum.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlatformsTest {
    @Test
    fun catalogDoesNotOfferGeminiOrDeferredPlatforms() {
        assertFalse(Platforms.byId(Platforms.GEMINI).addableThisSlice)
        assertFalse(Platforms.byId(Platforms.PERPLEXITY).addableThisSlice)
        assertFalse(Platforms.byId(Platforms.COPILOT).addableThisSlice)
        assertFalse(Platforms.byId(Platforms.M365).addableThisSlice)
        assertEquals(
            listOf(
                Platforms.CLAUDE,
                Platforms.CODEX,
                Platforms.GROK,
                Platforms.GROK_BOT,
                Platforms.CURSOR,
            ),
            Platforms.catalog.filter { it.addableThisSlice }.map { it.id },
        )
    }

    @Test
    fun grokBotSeedPlanIsIncludedWithSuperGrok() {
        assertEquals(
            "Included with SuperGrok",
            Platforms.byId(Platforms.GROK_BOT).seedPlans,
        )
    }
}
