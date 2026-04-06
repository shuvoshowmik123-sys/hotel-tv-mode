package com.hotelvision.launcher.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// ── Activation / Binding ─────────────────────────────────────────────────────
@Serializable
data class ActivateRequest(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("macAddress") val macAddress: String? = null
)

@Serializable
data class ActivationResponse(
    val pollToken: String,
    val activationCode: String,
    val deviceId: String,
    val macAddress: String? = null,
    val status: String
)

@Serializable
data class ActivationStatusResponse(
    val pollToken: String,
    val activationCode: String,
    val status: String,                     // "pending" | "bound"
    val sessionToken: String? = null,       // present when status == "bound"
    val roomNumber: String? = null          // present when status == "bound"
)

// ── Existing room info ────────────────────────────────────────────────────────
@Serializable
data class RoomInfoResponse(
    val roomNumber: String,
    val guestName: String? = null,
    val checkoutTime: String? = null,
    val status: String
)

// ── Phase 3: Admin Panel Config ───────────────────────────────────────────────

/**
 * Root Admin Panel configuration delivered from the hotel management system.
 * Polled every 5 minutes. Cached in Room on first successful response.
 */
@Serializable
data class AdminPanelConfigResponse(
    val hotel: HotelInfo,
    @SerialName("quick_settings") val quickSettings: QuickSettingsConfig,
    val screensaver: ScreensaverConfig,
    val menu: MenuConfig,
    @SerialName("local_guide") val localGuide: LocalGuideConfig,
    @SerialName("server_time") val serverTimeIso: String? = null,
    @SerialName("screensaver_timeout_ms") val screensaverTimeoutMs: Long = 120_000L  // 2 minutes default
)

@Serializable
data class HotelInfo(
    val name: String,
    val tagline: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("hero_image_url") val heroImageUrl: String? = null
)

@Serializable
data class QuickSettingsConfig(
    @SerialName("wifi_ssid") val wifiSsid: String = "",
    @SerialName("min_brightness_percent") val minBrightnessPercent: Int = 30,
    @SerialName("max_brightness_percent") val maxBrightnessPercent: Int = 100
)

@Serializable
data class ScreensaverConfig(
    val assets: List<String> = emptyList(),     // WebP image URLs
    @SerialName("transition_ms") val transitionMs: Long = 8_000L,
    @SerialName("weather_label") val weatherLabel: String? = null,
    @SerialName("temperature") val temperature: String? = null
)

@Serializable
data class MenuConfig(
    val categories: List<MenuCategory> = emptyList()
)

@Serializable
data class MenuCategory(
    val id: String,
    val name: String,
    @SerialName("meal_period") val mealPeriod: String,  // "breakfast", "lunch", "dinner", "late_night"
    @SerialName("available_from") val availableFrom: String,  // "HH:mm"
    @SerialName("available_until") val availableUntil: String,
    val items: List<MenuItemResponse> = emptyList()
)

@Serializable
data class MenuItemResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: String,  // formatted: "$12.99"
    @SerialName("image_url") val imageUrl: String? = null,
    val available: Boolean = true
)

@Serializable
data class LocalGuideConfig(
    val sections: List<LocalGuideSection> = emptyList()
)

@Serializable
data class LocalGuideSection(
    val id: String,
    val title: String,
    val items: List<LocalGuideItem> = emptyList()
)

@Serializable
data class LocalGuideItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val distance: String? = null  // "0.3 mi"
)

// ── Updated API Service ──────────────────────────────────────────────────────
interface LauncherApiService {
    @GET("api/launcher/room/{roomNum}")
    suspend fun getRoomInfo(@Path("roomNum") roomNum: String): RoomInfoResponse

    @GET("api/launcher/config")
    suspend fun getAdminPanelConfig(): AdminPanelConfigResponse

    /** Register this device with the server. Returns activation code + pollToken. */
    @POST("api/device/activate")
    suspend fun activate(@Body body: ActivateRequest): ActivationResponse

    /** Poll for binding status. Returns status="bound" with sessionToken when admin has bound it. */
    @GET("api/device/activation-status/{pollToken}")
    suspend fun getActivationStatus(@Path("pollToken") pollToken: String): ActivationStatusResponse

    /** Fetch launcher content using session token (after binding). */
    @GET("api/launcher/content")
    suspend fun getLauncherContent(@Query("sessionToken") sessionToken: String): AdminPanelConfigResponse
}
