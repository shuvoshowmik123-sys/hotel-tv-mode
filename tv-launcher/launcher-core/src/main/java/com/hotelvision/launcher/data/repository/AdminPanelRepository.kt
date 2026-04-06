package com.hotelvision.launcher.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hotelvision.launcher.data.api.ActivateRequest
import com.hotelvision.launcher.data.api.AdminPanelConfigResponse
import com.hotelvision.launcher.data.api.HotelInfo
import com.hotelvision.launcher.data.api.LauncherApiService
import com.hotelvision.launcher.data.api.LocalGuideConfig
import com.hotelvision.launcher.data.api.MenuConfig
import com.hotelvision.launcher.data.api.QuickSettingsConfig
import com.hotelvision.launcher.data.api.ScreensaverConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adminPanelDataStore by preferencesDataStore(name = "admin_panel_cache")

// DataStore keys
private val KEY_CONFIG_JSON     = stringPreferencesKey("config_json")
private val KEY_LAST_FETCH_MS   = longPreferencesKey("last_fetch_ms")
private val KEY_SESSION_TOKEN   = stringPreferencesKey("session_token")
private val KEY_POLL_TOKEN      = stringPreferencesKey("poll_token")
private val KEY_ACTIVATION_CODE = stringPreferencesKey("activation_code")
private val KEY_DEVICE_ID       = stringPreferencesKey("device_id")

/** State of the binding handshake */
sealed class BindingState {
    /** TV has never been bound — show lock screen with activation code */
    data class Unbound(
        val deviceId: String,
        val macAddress: String?,
        val activationCode: String
    ) : BindingState()

    /** TV is bound to a room — normal launcher interface */
    data class Bound(
        val sessionToken: String,
        val roomNumber: String
    ) : BindingState()

    /** Still determining state on first boot */
    object Loading : BindingState()
}

private val DefaultConfig = AdminPanelConfigResponse(
    hotel            = HotelInfo(name = "Hotel", tagline = null),
    quickSettings    = QuickSettingsConfig(),
    screensaver      = ScreensaverConfig(
        assets = listOf(
            "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1800&q=80"
        )
    ),
    menu             = MenuConfig(),
    localGuide       = LocalGuideConfig(),
    serverTimeIso    = null,
    screensaverTimeoutMs = 60_000L
)

/**
 * Manages device binding state and server-sent event (SSE) connection.
 *
 * Flow:
 *  1. On startup: read saved sessionToken from DataStore.
 *  2. If sessionToken exists → state = Bound → connect SSE → on "update" event, re-fetch config.
 *  3. If no sessionToken → call /api/device/activate → state = Unbound (lock screen shows code).
 *  4. Poll /api/device/activation-status every 8 seconds until status == "bound".
 *  5. Once bound → save sessionToken+roomNumber → connect SSE → transition to Bound state.
 *  6. On "unbound" SSE event → clear token → state = Unbound (lock screen returns).
 */
@Singleton
class AdminPanelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: LauncherApiService,
    private val json: Json
) {
    private val _config = MutableStateFlow(DefaultConfig)
    val config = _config.asStateFlow()

    private val _bindingState = MutableStateFlow<BindingState>(BindingState.Loading)
    val bindingState = _bindingState.asStateFlow()

    // OkHttp client with long read timeout for SSE
    private val sseHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // no timeout for SSE stream
        .build()

    private var sseSource: EventSource? = null

    /** Called from ViewModel.init via viewModelScope.launch */
    suspend fun startPolling() {
        loadCached()
        val prefs = context.adminPanelDataStore.data.firstOrNull()
        val savedSession = prefs?.get(KEY_SESSION_TOKEN)

        if (!savedSession.isNullOrBlank()) {
            // Already bound from a previous run
            val savedRoom = prefs.get(stringPreferencesKey("room_number")) ?: ""
            _bindingState.value = BindingState.Bound(savedSession, savedRoom)
            fetchAndCache(savedSession)
            connectSse(savedSession)
        } else {
            // First boot — register with server
            activateAndWait()
        }
    }

    /**
     * Registers this device with the server, shows unbound state,
     * then polls every 8 seconds until the admin binds it.
     */
    private suspend fun activateAndWait() {
        val deviceId = resolveDeviceId()
        val macAddress = resolveMacAddress()

        while (true) {
            try {
                val activation = api.activate(ActivateRequest(deviceId = deviceId, macAddress = macAddress))
                // Save poll token so we can check status
                context.adminPanelDataStore.edit { prefs ->
                    prefs[KEY_POLL_TOKEN] = activation.pollToken
                    prefs[KEY_ACTIVATION_CODE] = activation.activationCode
                    prefs[KEY_DEVICE_ID] = deviceId
                }
                // Show the lock screen
                _bindingState.value = BindingState.Unbound(
                    deviceId = deviceId,
                    macAddress = macAddress,
                    activationCode = activation.activationCode
                )
                // Poll for binding every 8 seconds
                pollForBinding(activation.pollToken, deviceId, macAddress, activation.activationCode)
                return // polling loop promoted to bound state
            } catch (e: Exception) {
                // Server unreachable — retry after 15 seconds
                e.printStackTrace()
                delay(15_000L)
            }
        }
    }

    /** Polls activation-status until the admin confirms binding */
    private suspend fun pollForBinding(
        pollToken: String,
        deviceId: String,
        macAddress: String?,
        activationCode: String
    ) {
        while (true) {
            try {
                val status = api.getActivationStatus(pollToken)
                if (status.status == "bound" && status.sessionToken != null) {
                    val roomNumber = status.roomNumber ?: ""
                    // Persist session token
                    context.adminPanelDataStore.edit { prefs ->
                        prefs[KEY_SESSION_TOKEN] = status.sessionToken
                        prefs[stringPreferencesKey("room_number")] = roomNumber
                    }
                    // Fetch content and connect SSE
                    fetchAndCache(status.sessionToken)
                    _bindingState.value = BindingState.Bound(status.sessionToken, roomNumber)
                    connectSse(status.sessionToken)
                    return
                }
            } catch (e: Exception) {
                // 404 = token gone (maybe admin cleared it) — restart activate
                e.printStackTrace()
            }
            delay(8_000L)
        }
    }

    /** Opens an SSE connection to /api/launcher/events and handles server-push events */
    private fun connectSse(sessionToken: String) {
        sseSource?.cancel()

        val baseUrl = context.resources.getString(
            context.resources.getIdentifier("admin_panel_base_url", "string", context.packageName)
        ).ifBlank { "https://hotel-tv-mode.vercel.app/" }

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/launcher/events?sessionToken=$sessionToken")
            .addHeader("Accept", "text/event-stream")
            .build()

        sseSource = EventSources.createFactory(sseHttpClient).newEventSource(
            request = request,
            listener = object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    when (data.trim()) {
                        "update" -> {
                            // Admin changed something — re-fetch config immediately
                            kotlinx.coroutines.runBlocking {
                                fetchAndCache(sessionToken)
                            }
                        }
                        "unbound" -> {
                            // Admin unbound this device — clear state and show lock screen
                            kotlinx.coroutines.runBlocking {
                                clearBinding()
                                activateAndWait()
                            }
                        }
                        "ping", "connected" -> { /* keep-alive, ignore */ }
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    // Reconnect after 10 seconds
                    kotlinx.coroutines.runBlocking {
                        delay(10_000L)
                        connectSse(sessionToken)
                    }
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    t?.printStackTrace()
                    // Reconnect after 15 seconds on failure
                    kotlinx.coroutines.runBlocking {
                        delay(15_000L)
                        connectSse(sessionToken)
                    }
                }
            }
        )
    }

    /** Clears saved binding data from DataStore */
    private suspend fun clearBinding() {
        context.adminPanelDataStore.edit { prefs ->
            prefs.remove(KEY_SESSION_TOKEN)
            prefs.remove(KEY_POLL_TOKEN)
            prefs.remove(KEY_ACTIVATION_CODE)
            prefs.remove(stringPreferencesKey("room_number"))
        }
    }

    private suspend fun fetchAndCache(sessionToken: String? = null) {
        try {
            val response = if (!sessionToken.isNullOrBlank()) {
                // Prefer content endpoint (room-specific) when bound
                runCatching { api.getLauncherContent(sessionToken) }.getOrNull()
                    ?: api.getAdminPanelConfig()
            } else {
                api.getAdminPanelConfig()
            }
            if (response.toVisibleUiConfig() != _config.value.toVisibleUiConfig()) {
                _config.value = response
            }
            val encoded = json.encodeToString(response)
            context.adminPanelDataStore.edit { prefs ->
                prefs[KEY_CONFIG_JSON]   = encoded
                prefs[KEY_LAST_FETCH_MS] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadCached() {
        try {
            val prefs = context.adminPanelDataStore.data.firstOrNull() ?: return
            val cachedJson = prefs[KEY_CONFIG_JSON] ?: return
            runCatching {
                _config.value = json.decodeFromString(cachedJson)
            }.onFailure { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Returns a stable device ID (Android ID) */
    private fun resolveDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "device-${System.currentTimeMillis()}"
    }

    /** Returns MAC address if WifiManager is available */
    private fun resolveMacAddress(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.macAddress?.takeIf { it != "02:00:00:00:00:00" }
        } catch (_: Exception) {
            null
        }
    }
}

private fun AdminPanelConfigResponse.toVisibleUiConfig(): AdminPanelConfigResponse {
    return copy(serverTimeIso = null)
}
