package com.ericmbpeck.ai_sum.wearbridge

import org.junit.Assert.assertEquals
import org.junit.Test

class WearDataPathsTest {
    @Test
    fun openUriIsTheBrowsableDeepLinkThePhoneFilterMustMatch() {
        assertEquals("/aisum/open", WearDataPaths.OPEN)
        assertEquals("aisum", WearDataPaths.OPEN_SCHEME)
        assertEquals("open", WearDataPaths.OPEN_HOST)
        assertEquals("aisum://open", WearDataPaths.OPEN_URI)
    }
}
