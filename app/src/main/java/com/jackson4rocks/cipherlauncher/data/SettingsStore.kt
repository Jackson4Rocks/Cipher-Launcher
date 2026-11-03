package com.jackson4rocks.cipherlauncher.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("cipher_settings", Context.MODE_PRIVATE)

    var amoled: Boolean
        get() = prefs.getBoolean(KEY_AMOLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED, value).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(KEY_SECONDS, true)
        set(value) = prefs.edit().putBoolean(KEY_SECONDS, value).apply()

    var useWallpaper: Boolean
        get() = prefs.getBoolean(KEY_USE_WALLPAPER, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_WALLPAPER, value).apply()

    var wallpaperUri: String?
        get() = prefs.getString(KEY_WALLPAPER_URI, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER_URI, value).apply()

    var hideBranding: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BRANDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_BRANDING, value).apply()

    private companion object {
        const val KEY_AMOLED = "amoled"
        const val KEY_SECONDS = "show_seconds"
        const val KEY_USE_WALLPAPER = "use_wallpaper"
        const val KEY_WALLPAPER_URI = "wallpaper_uri"
        const val KEY_HIDE_BRANDING = "hide_branding"
    }
}
