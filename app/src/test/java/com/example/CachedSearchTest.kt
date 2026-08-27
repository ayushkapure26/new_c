package com.example

import com.example.data.model.CachedSearch
import com.example.util.AppLocalization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedSearchTest {

    @Test
    fun testLocalizationStringsAvailable() {
        val english = AppLocalization.getStrings("English")
        assertEquals("CNGमित्र", english.appTitle)
        assertEquals("Search pump, area, or road...", english.searchPlaceholder)
        assertEquals("⚡ Offline Map Cache Active", english.offlineCacheActive)

        val hindi = AppLocalization.getStrings("Hindi (हिंदी)")
        assertEquals("CNGमित्र", hindi.appTitle)
        assertEquals("पंप, क्षेत्र या मार्ग खोजें...", hindi.searchPlaceholder)
        assertEquals("⚡ ऑफलाइन मैप कैश सक्रिय", hindi.offlineCacheActive)
    }

    @Test
    fun testCachedSearchEntity() {
        val cached = CachedSearch(
            id = 1L,
            query = "Connaught Place",
            city = "Delhi NCR",
            latitude = 28.6315,
            longitude = 77.2167,
            timestamp = System.currentTimeMillis(),
            resultCount = 3,
            previewStationNames = "IGL Connaught Place"
        )
        assertEquals("Connaught Place", cached.query)
        assertEquals("Delhi NCR", cached.city)
        assertEquals(3, cached.resultCount)
        assertTrue(cached.previewStationNames.contains("IGL"))
    }
}
