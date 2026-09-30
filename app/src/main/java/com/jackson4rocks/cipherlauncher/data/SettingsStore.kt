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

    private companion object {
        const val KEY_AMOLED = "amoled"
        const val KEY_SECONDS = "show_seconds"
    }
}
