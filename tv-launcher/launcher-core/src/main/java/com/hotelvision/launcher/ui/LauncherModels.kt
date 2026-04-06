package com.hotelvision.launcher.ui

import android.content.Intent
import com.hotelvision.launcher.data.api.*
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity

// ACTION_SHOW_INPUTS removed — Inputs is a consumer feature, forbidden in hotel build
const val ACTION_SHOW_ROOM_SERVICE = "com.hotelvision.launcher.action.ROOM_SERVICE"
const val ACTION_SHOW_LOCAL_GUIDE = "com.hotelvision.launcher.action.LOCAL_GUIDE"

enum class LauncherDestination(
    val title: String,
    val intentAction: String?
) {
    HOME(title = "Home", intentAction = null),
    ROOM_SERVICE(title = "Room Service", intentAction = ACTION_SHOW_ROOM_SERVICE),
    LOCAL_GUIDE(title = "Local Guide", intentAction = ACTION_SHOW_LOCAL_GUIDE);

    companion object {
        fun fromAction(action: String?): LauncherDestination {
            return entries.firstOrNull { it.intentAction == action } ?: HOME
        }
    }
}

enum class AppMoveDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

enum class HomeSectionStyle {
    STANDARD,
    COMPACT
}

enum class HomeFeedRowType {
    DINING_TIME_AWARE,
    APP_RECOMMENDATIONS,
    HOTEL_FEATURES,
    APPS_INSTALLED,
    ALERT
}

enum class LauncherCardType {
    FEATURED,
    APP,
    DINING,
    SERVICE,
    SOURCE,
    ALERT,
    DESTINATION,
    RECOMMENDATION
}

enum class MealPeriod {
    BREAKFAST,
    LUNCH,
    DINNER,
    LATE_NIGHT,
    /** Outside defined meal windows — hides the food row */
    OFF_HOURS
}

sealed interface LauncherAction {
    data object None : LauncherAction
    data object OpenNotificationsPanel : LauncherAction
    data object OpenControlPanel : LauncherAction
    data object ExitTransientUi : LauncherAction
    data object ResetGuestPersonalization : LauncherAction
    data object OpenAllApps : LauncherAction

    data class OpenDestination(
        val destination: LauncherDestination
    ) : LauncherAction

    data class EnterAppMoveMode(
        val packageName: String
    ) : LauncherAction

    data class LaunchPackage(
        val packageName: String
    ) : LauncherAction

    data class LaunchIntent(
        val intent: Intent
    ) : LauncherAction
}

data class HotelBranding(
    val hotelName: String,
    val shortBrand: String,
    val tagline: String,
    val location: String
)

data class AmbientBackdropState(
    val title: String,
    val imageUrl: String,
    val overlayColor: Long = 0xFF07101A,
    val slideshowImages: List<String> = emptyList()
)

data class DestinationBackdropsState(
    val home: AmbientBackdropState = AmbientBackdropState(
        title = "Hotel",
        imageUrl = "",
        slideshowImages = emptyList()
    ),
    val roomService: AmbientBackdropState = home,
    val localGuide: AmbientBackdropState = home
) {
    fun forDestination(destination: LauncherDestination): AmbientBackdropState {
        return when (destination) {
            LauncherDestination.HOME -> home
            LauncherDestination.ROOM_SERVICE -> roomService
            LauncherDestination.LOCAL_GUIDE -> localGuide
        }
    }
}

data class HotelFeatureCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val supportingText: String,
    val imageUrl: String? = null,
    val ambientImageUrl: String? = null,
    val badge: String,
    val accentColor: Long,
    val action: LauncherAction = LauncherAction.None
)

data class HotelFeatureSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val style: HomeSectionStyle,
    val enabled: Boolean = true,
    val cards: List<HotelFeatureCard>
)

data class RecommendationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val artUrl: String,
    val ambientUrl: String,
    val sourceApp: String,
    val badge: String,
    val accentColor: Long,
    val action: LauncherAction
)

data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val badge: String,
    val action: LauncherAction
)

data class InstalledAppItem(
    val id: String,
    val packageName: String,
    val launchActivityClassName: String? = null,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isSystemApp: Boolean,
    val action: LauncherAction
)

data class SourceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val isSystemProvided: Boolean,
    val action: LauncherAction
)

data class GuestMessageItem(
    val id: String,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val badge: String,
    val action: LauncherAction
)

data class HomeCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val supportingText: String,
    val imageUrl: String? = null,
    val ambientImageUrl: String? = null,
    val sourceLabel: String? = null,
    val packageName: String? = null,
    val launchActivityClassName: String? = null,
    val badge: String,
    val accentColor: Long,
    val cardType: LauncherCardType,
    val action: LauncherAction
)

data class HomeFeedRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val rowType: HomeFeedRowType,
    val style: HomeSectionStyle,
    val cards: List<HomeCard>
)

data class LauncherUiState(
    val selectedDestination: LauncherDestination = LauncherDestination.HOME,
    val hotelBranding: HotelBranding = HotelBranding(
        hotelName = "Hotel",
        shortBrand = "H",
        tagline = "Hotel & Residences",
        location = "Local"
    ),
    val roomInfo: RoomInfoEntity? = null,
    val adminConfig: AdminPanelConfigResponse? = null,
    val welcomeTitle: String = "Welcome",
    val welcomeSubtitle: String = "Curated for your stay",
    val mealPeriod: MealPeriod = MealPeriod.BREAKFAST,
    val ambientBackdrop: AmbientBackdropState = AmbientBackdropState(
        title = "Hotel",
        imageUrl = "",
        slideshowImages = emptyList()
    ),
    val destinationBackdrops: DestinationBackdropsState = DestinationBackdropsState(),
    val feedRows: List<HomeFeedRow> = emptyList(),
    val guestMessages: List<GuestMessageItem> = emptyList(),
    val simpleModeEnabled: Boolean = false,
    val allInstalledApps: List<InstalledAppItem> = emptyList(),
    /** Current binding state — drives whether the unbound lock screen is shown */
    val bindingState: com.hotelvision.launcher.data.repository.BindingState =
        com.hotelvision.launcher.data.repository.BindingState.Loading
)

data class RoomSession(
    val roomId: String,
    val checkInTimeMs: Long,
    val guestName: String? = null
)
