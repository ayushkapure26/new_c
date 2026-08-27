package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.AppLocalization
import com.example.util.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocaleManagerTest {

    @Test
    fun testLocaleManagerSingleton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager1 = LocaleManager.getInstance(context)
        val manager2 = LocaleManager.getInstance(context)
        assertNotNull(manager1)
        assertEquals(manager1, manager2)
    }

    @Test
    fun testLanguageSettingAndFlowEmission() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = LocaleManager.getInstance(context)

        // Set to Marathi
        manager.setLanguage("Marathi (मराठी)")
        val currentLang = manager.selectedLanguageFlow.first()
        assertEquals("Marathi (मराठी)", currentLang)

        val strings = manager.getLocalizedStrings(currentLang)
        assertEquals("CNGमित्र", strings.appTitle)
        assertTrue(strings.nearbyStations.isNotEmpty())

        // Set to English
        manager.setLanguage("English")
        val englishLang = manager.selectedLanguageFlow.first()
        assertEquals("English", englishLang)
        val englishStrings = manager.getLocalizedStrings(englishLang)
        assertEquals("Nearby CNG Stations", englishStrings.nearbyStations)
    }

    @Test
    fun testAllSupportedLanguagesLocalization() {
        val supported = listOf(
            "English",
            "Hindi (हिंदी)",
            "Marathi (मराठी)",
            "Gujarati (ગુજરાતી)",
            "Punjabi (ਪੰਜਾਬੀ)",
            "Tamil (தமிழ்)",
            "Telugu (తెలుగు)",
            "Bengali (বাংলা)",
            "Kannada (ಕನ್ನಡ)",
            "Spanish (Español)"
        )

        for (lang in supported) {
            val localized = AppLocalization.getStrings(lang)
            assertNotNull("Localized strings should exist for $lang", localized)
            assertTrue("App title should not be blank for $lang", localized.appTitle.isNotBlank())
            assertTrue("Select language string should not be blank for $lang", localized.selectLanguage.isNotBlank())
        }
    }
}
