package com.hotelvision.launcher.setup

import android.content.Context

object DefaultLauncherPromptStore {
    private const val PREFS_NAME = "default_launcher_prompt"
    private const val KEY_PROMPT_PENDING = "prompt_pending"
    private const val KEY_ROLE_ATTEMPTED = "role_attempted"
    private const val KEY_HOME_CHOOSER_ATTEMPTED = "home_chooser_attempted"
    private const val KEY_VENDOR_SETTINGS_ATTEMPTED = "vendor_settings_attempted"

    fun isPromptPending(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_PROMPT_PENDING, false)
    }

    fun markPromptPending(context: Context, pending: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROMPT_PENDING, pending).apply()
    }

    internal fun progress(context: Context): DefaultLauncherProgress {
        val prefs = prefs(context)
        return DefaultLauncherProgress(
            roleAttempted = prefs.getBoolean(KEY_ROLE_ATTEMPTED, false),
            homeChooserAttempted = prefs.getBoolean(KEY_HOME_CHOOSER_ATTEMPTED, false),
            vendorSettingsAttempted = prefs.getBoolean(KEY_VENDOR_SETTINGS_ATTEMPTED, false)
        )
    }

    fun markRoleAttempted(context: Context) {
        prefs(context).edit().putBoolean(KEY_ROLE_ATTEMPTED, true).apply()
    }

    fun markHomeChooserAttempted(context: Context) {
        prefs(context).edit().putBoolean(KEY_HOME_CHOOSER_ATTEMPTED, true).apply()
    }

    fun markVendorSettingsAttempted(context: Context) {
        prefs(context).edit().putBoolean(KEY_VENDOR_SETTINGS_ATTEMPTED, true).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
