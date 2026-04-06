package com.hotelvision.launcher.data.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GuestLauncherPreferences(
    val appOrder: List<String> = emptyList()
)

@Singleton
class GuestPersonalizationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences = GuestPersonalizationStore.preferences(context)
    private val state = MutableStateFlow(GuestPersonalizationStore.read(preferences))

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == GuestPersonalizationStore.KEY_APP_ORDER) {
            state.value = GuestPersonalizationStore.read(preferences)
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun observeState(): StateFlow<GuestLauncherPreferences> = state.asStateFlow()

    fun saveAppOrder(order: List<String>) {
        GuestPersonalizationStore.writeAppOrder(preferences, order)
        state.value = GuestPersonalizationStore.read(preferences)
    }

    fun clear() {
        GuestPersonalizationStore.clear(context)
        state.value = GuestLauncherPreferences()
    }
}

internal object GuestPersonalizationStore {
    private const val PREFS_NAME = "guest_personalization"
    internal const val KEY_APP_ORDER = "app_order"
    private const val DELIMITER = "|"

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(preferences: SharedPreferences): GuestLauncherPreferences {
        val order = preferences.getString(KEY_APP_ORDER, null)
            ?.split(DELIMITER)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return GuestLauncherPreferences(appOrder = order)
    }

    fun writeAppOrder(preferences: SharedPreferences, order: List<String>) {
        preferences.edit()
            .putString(KEY_APP_ORDER, order.joinToString(DELIMITER))
            .apply()
    }

    fun clear(context: Context) {
        preferences(context).edit().clear().apply()
    }
}
