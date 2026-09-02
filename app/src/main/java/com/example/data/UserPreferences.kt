package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_preferences")

data class BrowserSettings(
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val adBlockingEnabled: Boolean = true,
    val trackerBlockingEnabled: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val allowCookies: Boolean = true,
    val doNotTrack: Boolean = true,
    val httpsOnlyMode: Boolean = true,
    val desktopModeDefault: Boolean = false,
    val aiEnabled: Boolean = true,
    val aiConfirmBeforeSend: Boolean = true,
    val aiDefaultLanguage: String = "English",
    val customGeminiApiKey: String = "",
    val darkThemeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val allowedAdSites: Set<String> = emptySet()
)

class UserPreferences(private val context: Context) {

    private val KEY_SEARCH_ENGINE = stringPreferencesKey("search_engine")
    private val KEY_AD_BLOCK = booleanPreferencesKey("ad_block")
    private val KEY_TRACKER_BLOCK = booleanPreferencesKey("tracker_block")
    private val KEY_BLOCK_3RD_PARTY_COOKIES = booleanPreferencesKey("block_3rd_party_cookies")
    private val KEY_ALLOW_COOKIES = booleanPreferencesKey("allow_cookies")
    private val KEY_DO_NOT_TRACK = booleanPreferencesKey("do_not_track")
    private val KEY_HTTPS_ONLY = booleanPreferencesKey("https_only")
    private val KEY_DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
    private val KEY_AI_ENABLED = booleanPreferencesKey("ai_enabled")
    private val KEY_AI_CONFIRM = booleanPreferencesKey("ai_confirm")
    private val KEY_AI_LANGUAGE = stringPreferencesKey("ai_language")
    private val KEY_CUSTOM_GEMINI_KEY = stringPreferencesKey("custom_gemini_key")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_ALLOWED_AD_SITES = stringSetPreferencesKey("allowed_ad_sites")

    val settingsFlow: Flow<BrowserSettings> = context.dataStore.data.map { pref ->
        BrowserSettings(
            searchEngine = SearchEngine.fromName(pref[KEY_SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.name),
            adBlockingEnabled = pref[KEY_AD_BLOCK] ?: true,
            trackerBlockingEnabled = pref[KEY_TRACKER_BLOCK] ?: true,
            blockThirdPartyCookies = pref[KEY_BLOCK_3RD_PARTY_COOKIES] ?: true,
            allowCookies = pref[KEY_ALLOW_COOKIES] ?: true,
            doNotTrack = pref[KEY_DO_NOT_TRACK] ?: true,
            httpsOnlyMode = pref[KEY_HTTPS_ONLY] ?: true,
            desktopModeDefault = pref[KEY_DESKTOP_MODE] ?: false,
            aiEnabled = pref[KEY_AI_ENABLED] ?: true,
            aiConfirmBeforeSend = pref[KEY_AI_CONFIRM] ?: true,
            aiDefaultLanguage = pref[KEY_AI_LANGUAGE] ?: "English",
            customGeminiApiKey = pref[KEY_CUSTOM_GEMINI_KEY] ?: "",
            darkThemeMode = pref[KEY_THEME_MODE] ?: "SYSTEM",
            allowedAdSites = pref[KEY_ALLOWED_AD_SITES] ?: emptySet()
        )
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.dataStore.edit { it[KEY_SEARCH_ENGINE] = engine.name }
    }

    suspend fun setAdBlocking(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AD_BLOCK] = enabled }
    }

    suspend fun setTrackerBlocking(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TRACKER_BLOCK] = enabled }
    }

    suspend fun setBlockThirdPartyCookies(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BLOCK_3RD_PARTY_COOKIES] = enabled }
    }

    suspend fun setAllowCookies(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALLOW_COOKIES] = enabled }
    }

    suspend fun setDoNotTrack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DO_NOT_TRACK] = enabled }
    }

    suspend fun setHttpsOnly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HTTPS_ONLY] = enabled }
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DESKTOP_MODE] = enabled }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AI_ENABLED] = enabled }
    }

    suspend fun setAiConfirmBeforeSend(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AI_CONFIRM] = enabled }
    }

    suspend fun setAiDefaultLanguage(lang: String) {
        context.dataStore.edit { it[KEY_AI_LANGUAGE] = lang }
    }

    suspend fun setCustomGeminiApiKey(key: String) {
        context.dataStore.edit { it[KEY_CUSTOM_GEMINI_KEY] = key.trim() }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun toggleSiteAdBlock(host: String, allow: Boolean) {
        context.dataStore.edit { pref ->
            val current = pref[KEY_ALLOWED_AD_SITES]?.toMutableSet() ?: mutableSetOf()
            if (allow) {
                current.add(host.lowercase())
            } else {
                current.remove(host.lowercase())
            }
            pref[KEY_ALLOWED_AD_SITES] = current
        }
    }
}
