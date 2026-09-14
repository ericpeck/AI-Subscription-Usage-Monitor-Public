package com.ericmbpeck.ai_sum.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerScreenTest {
    @Test
    fun shortPlanTakesTheFirstSegment() {
        assertEquals("Max 5×", shortPlan("Max 5× · rolling session and weekly caps"))
        assertEquals("Ultra", shortPlan("Ultra · personal · on the tab bar"))
        assertEquals("Included with SuperGrok", shortPlan("Included with SuperGrok"))
        assertEquals("Pro", shortPlan("Pro · rolling session and weekly caps"))
        assertEquals("Pro", shortPlan("Pro\nsecond line"))
    }
}
