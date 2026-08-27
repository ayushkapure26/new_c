package com.example.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

// DataStore extension property on Context
val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(name = "cng_locale_preferences")

/**
 * Persistent LocaleManager using Jetpack DataStore to save and manage application language.
 * Ensures the chosen locale is applied globally across the app and survives navigation,
 * screen reloads, configuration changes, and app restarts.
 */
class LocaleManager private constructor(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val SELECTED_LANGUAGE_KEY = stringPreferencesKey("selected_language")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        const val DEFAULT_LANGUAGE = "English"

        @Volatile
        private var INSTANCE: LocaleManager? = null

        fun getInstance(context: Context): LocaleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocaleManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * CompositionLocal for accessing LocaleManager anywhere in Compose tree.
         */
        val LocalLocaleManager = staticCompositionLocalOf<LocaleManager> {
            error("LocaleManager not provided. Wrap your content in ProvideAppLocale.")
        }

        /**
         * CompositionLocal for accessing the current LocalizedStrings anywhere in Compose tree.
         */
        val LocalAppStrings = compositionLocalOf {
            AppLocalization.getStrings(DEFAULT_LANGUAGE)
        }

        /**
         * CompositionLocal for accessing the raw language name anywhere in Compose tree.
         */
        val LocalAppLanguage = compositionLocalOf {
            DEFAULT_LANGUAGE
        }
    }

    /**
     * Flow observing selected language from DataStore.
     */
    val selectedLanguageFlow: Flow<String> = context.localeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
        }

    /**
     * Eager StateFlow with the current language for immediate UI rendering.
     */
    val selectedLanguageState: StateFlow<String> = selectedLanguageFlow
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly,
            initialValue = DEFAULT_LANGUAGE
        )

    /**
     * Flow of LocalizedStrings corresponding to the saved language.
     */
    val localizedStringsFlow: Flow<LocalizedStrings> = selectedLanguageFlow
        .map { lang -> AppLocalization.getStrings(lang) }

    /**
     * Save selected language persistently to DataStore.
     */
    suspend fun setLanguage(language: String) {
        context.localeDataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE_KEY] = language
        }
        applySystemLocale(language)
    }

    /**
     * Non-suspending async setter for easy invocation from UI callbacks.
     */
    fun setLanguageAsync(language: String) {
        applicationScope.launch {
            setLanguage(language)
        }
    }

    /**
     * Retrieve LocalizedStrings for a specific or current language.
     */
    fun getLocalizedStrings(language: String = selectedLanguageState.value): LocalizedStrings {
        return AppLocalization.getStrings(language)
    }

    /**
     * Helper to set JVM default locale for date/number formatting consistency.
     */
    private fun applySystemLocale(language: String) {
        val locale = when {
            language.startsWith("Hindi") || language.contains("हिंदी") -> Locale("hi", "IN")
            language.startsWith("Marathi") || language.contains("मराठी") -> Locale("mr", "IN")
            language.startsWith("Gujarati") || language.contains("ગુજરાતી") -> Locale("gu", "IN")
            language.startsWith("Punjabi") || language.contains("ਪੰਜਾਬੀ") -> Locale("pa", "IN")
            language.startsWith("Tamil") || language.contains("தமிழ்") -> Locale("ta", "IN")
            language.startsWith("Telugu") || language.contains("తెలుగు") -> Locale("te", "IN")
            language.startsWith("Bengali") || language.contains("বাংলা") -> Locale("bn", "IN")
            language.startsWith("Kannada") || language.contains("ಕನ್ನಡ") -> Locale("kn", "IN")
            language.startsWith("Spanish") || language.contains("Español") -> Locale("es", "ES")
            else -> Locale("en", "US")
        }
        try {
            Locale.setDefault(locale)
        } catch (_: Exception) {}
    }
}

/**
 * Top-level Composable wrapper providing LocaleManager, current language,
 * and reactive LocalizedStrings throughout the Compose hierarchy.
 */
@Composable
fun ProvideAppLocale(
    localeManager: LocaleManager,
    content: @Composable () -> Unit
) {
    val currentLanguage by localeManager.selectedLanguageFlow.collectAsState(
        initial = localeManager.selectedLanguageState.value
    )
    val localizedStrings = AppLocalization.getStrings(currentLanguage)

    CompositionLocalProvider(
        LocaleManager.LocalLocaleManager provides localeManager,
        LocaleManager.LocalAppLanguage provides currentLanguage,
        LocaleManager.LocalAppStrings provides localizedStrings
    ) {
        content()
    }
}

/**
 * Convenient accessor property in any Composable to get the current LocalizedStrings.
 */
val localizedAppStrings: LocalizedStrings
    @Composable
    @ReadOnlyComposable
    get() = LocaleManager.LocalAppStrings.current

/**
 * Convenient accessor property in any Composable to get the current selected language name.
 */
val currentAppLanguage: String
    @Composable
    @ReadOnlyComposable
    get() = LocaleManager.LocalAppLanguage.current
