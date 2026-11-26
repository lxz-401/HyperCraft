package com.sevtinge.hyperceiler.settings

/**
 * Centralized preference keys to avoid stringly-typed access.
 * All keys must match the keys defined in res/xml/prefs_settings.xml
 */
object PreferencesKeys {
    // Icon settings
    const val ICON_MODE = "prefs_key_settings_icon"
    const val ICON_MODE_VALUE = "prefs_key_settings_icon_mode"

    // App appearance
    const val APP_LANGUAGE = "prefs_key_settings_app_language"
    const val HIDE_APP_ICON = "prefs_key_settings_hide_app_icon"
    const val FLOAT_NAV = "prefs_key_settings_float_nav"

    // Logging
    const val LOG_LEVEL = "prefs_key_log_level_v2"

    // Scope management
    const val SCOPE_SYNC = "prefs_key_settings_scope_sync"
    const val SCOPE = "prefs_key_settings_scope"

    // Developer features
    const val HOT_RELOAD = "prefs_key_settings_hot_reload"

    // Backup & Restore
    const val BACKUP = "prefs_key_back"
    const val RESTORE = "prefs_key_rest"
    const val RESET = "prefs_key_reset"
}