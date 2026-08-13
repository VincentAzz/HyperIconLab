package com.capybara.hypericonlab.modules.settings.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppDataStore(
    private val dataStore: DataStore<Preferences>
) {
    val data: Flow<Preferences> = dataStore.data

    companion object {
        val UI_USE_BLUR = booleanPreferencesKey("ui_use_blur")
        val UI_USE_LIQUID_GLASS_BOTTOM_SHEET =
            booleanPreferencesKey("ui_use_liquid_glass_bottom_sheet")
        val UI_LIQUID_GLASS_ENGINE = stringPreferencesKey("ui_liquid_glass_engine")
        val UI_USE_CUSTOM_LIQUID_GLASS_ENGINE =
            booleanPreferencesKey("ui_use_custom_liquid_glass_engine")
        val KYANT_GLASS_BLUR_SCALE = floatPreferencesKey("kyant_glass_blur_scale")
        val KYANT_GLASS_REFRACTION_HEIGHT_SCALE =
            floatPreferencesKey("kyant_glass_refraction_height_scale")
        val KYANT_GLASS_REFRACTION_AMOUNT_SCALE =
            floatPreferencesKey("kyant_glass_refraction_amount_scale")
        val KYANT_GLASS_CHROMATIC_ABERRATION =
            floatPreferencesKey("kyant_glass_chromatic_aberration")
        val UI_USE_SMOOTHER_ROUNDED_CORNERS = booleanPreferencesKey("ui_use_smoother_rounded_corners")
        val UI_USE_CUSTOM_CARD_CORNER_RADIUS =
            booleanPreferencesKey("ui_use_custom_card_corner_radius")
        val UI_CARD_CORNER_SIZE = stringPreferencesKey("ui_card_corner_size")
        val UI_USE_APPLE_STYLE_CARD = booleanPreferencesKey("ui_use_apple_style_card")
        val UI_USE_SHEET_CARD_BACKGROUND =
            booleanPreferencesKey("ui_use_sheet_card_background")
        val UI_USE_APPLE_STYLE_TOGGLE = booleanPreferencesKey("ui_use_apple_style_toggle")
        val UI_USE_APPLE_STYLE_SLIDER = booleanPreferencesKey("ui_use_apple_style_slider")
        val UI_USE_GOOGLE_SANS_FLEX = booleanPreferencesKey("ui_use_google_sans_flex")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE_STYLE = stringPreferencesKey("theme_palette_style")
        val THEME_COLOR_SPEC = stringPreferencesKey("theme_color_spec")
        val THEME_USE_DYNAMIC_COLOR = booleanPreferencesKey("theme_use_dynamic_color")
        val THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
        val UI_USE_FLOATING_BOTTOM_BAR = booleanPreferencesKey("ui_use_floating_bottom_bar")
        val UI_USE_FLOATING_BOTTOM_BAR_BLUR =
            booleanPreferencesKey("ui_use_floating_bottom_bar_blur")
        val UI_USE_FLOATING_BAR_COMPACT =
            booleanPreferencesKey("ui_use_floating_bottom_bar_compact")
        val UI_USE_PROGRESSIVE_BLUR_TOP_APP_BAR =
            booleanPreferencesKey("ui_use_progressive_blur_top_app_bar")
        val UI_USE_TAB_ROW_CENTER_ALIGNMENT =
            booleanPreferencesKey("ui_use_tab_row_center_alignment")
        val UI_USE_TAB_ROW_TRANSPARENT_BACKGROUND =
            booleanPreferencesKey("ui_use_tab_row_transparent_background")
        val UI_USE_TAB_ROW_FILL_WIDTH =
            booleanPreferencesKey("ui_use_tab_row_fill_width")
        val UI_FLOATING_BAR_COMPACT_TYPE =
            stringPreferencesKey("ui_floating_bottom_bar_compact_type")
        val LAST_MAIN_PAGE_INDEX = intPreferencesKey("last_main_page_index")
        val UI_USE_DOWNLOAD_PROXY = booleanPreferencesKey("ui_use_download_proxy")
        val INITIALIZATION_COMPLETED = booleanPreferencesKey("initialization_completed")
        val INITIALIZATION_COMPLETED_TASKS = stringPreferencesKey("initialization_completed_tasks")
        val INITIALIZATION_ACTIVE_TASK = stringPreferencesKey("initialization_active_task")
        val INITIALIZATION_RESOURCE_VERSION =
            stringPreferencesKey("initialization_resource_version")
        val INITIALIZATION_TEMPLATE_VERSION =
            stringPreferencesKey("initialization_template_version")
        val INITIALIZATION_CACHE_SOURCE_VERSION =
            stringPreferencesKey("initialization_cache_source_version")
        val INITIALIZATION_CACHE_CONFIG_VERSION =
            stringPreferencesKey("initialization_cache_config_version")
        val INITIALIZATION_FAILED_TASK = stringPreferencesKey("initialization_failed_task")
        val INITIALIZATION_FAILURE_MESSAGE =
            stringPreferencesKey("initialization_failure_message")
        val INITIALIZATION_FAILURE_AT = longPreferencesKey("initialization_failure_at")
        val INITIALIZATION_REQUIRES_MANUAL_START =
            booleanPreferencesKey("initialization_requires_manual_start")
        val ASSET_CHECK_LAST_AUTOMATIC_AT = longPreferencesKey("asset_check_last_automatic_at")
        val ASSET_CHECK_LAST_MANUAL_AT = longPreferencesKey("asset_check_last_manual_at")
        val ASSET_CHECK_STATE = stringPreferencesKey("asset_check_state")
    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    fun getString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { it[key] = value }
    }

    fun getInt(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    fun getBoolean(key: Preferences.Key<Boolean>, default: Boolean = false): Flow<Boolean> =
        dataStore.data.map { it[key] ?: default }

    suspend fun putFloat(key: Preferences.Key<Float>, value: Float) {
        dataStore.edit { it[key] = value }
    }

    fun getFloat(key: Preferences.Key<Float>, default: Float = 0f): Flow<Float> =
        dataStore.data.map { it[key] ?: default }

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        dataStore.edit(transform)
    }
}
