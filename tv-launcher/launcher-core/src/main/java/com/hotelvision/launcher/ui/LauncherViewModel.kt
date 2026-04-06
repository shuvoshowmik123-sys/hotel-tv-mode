package com.hotelvision.launcher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hotelvision.launcher.data.db.dao.LauncherDao
import com.hotelvision.launcher.data.db.entities.LauncherRowEntity
import com.hotelvision.launcher.data.db.entities.MenuItemEntity
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity
import com.hotelvision.launcher.data.db.entities.ServiceEntity
import com.hotelvision.launcher.data.repository.LauncherRepository
import com.hotelvision.launcher.data.session.GuestLauncherPreferences
import com.hotelvision.launcher.data.session.GuestPersonalizationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.hotelvision.launcher.data.repository.AdminPanelRepository
import com.hotelvision.launcher.data.api.AdminPanelConfigResponse

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repository: LauncherRepository,
    private val dao: LauncherDao,
    private val guestPersonalizationManager: GuestPersonalizationManager,
    private val adminPanelRepository: AdminPanelRepository
) : ViewModel() {

    // ── Phase 6: Screensaver properties exposed to MainActivity ───────────────
    val screensaverTimeoutMs: Long
        get() {
            val configValue = adminPanelRepository.config.value.screensaverTimeoutMs
            return if (configValue > 0) configValue else 60_000L
        }

    val screensaverImages: List<String>
        get() = adminPanelRepository.config.value.screensaver.assets.ifEmpty {
            listOf(
                "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1800&q=80",
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1800&q=80",
                "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=1800&q=80",
                "https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1800&q=80"
            )
        }


    private val roomInfo: StateFlow<RoomInfoEntity?> = repository.getRoomInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val rows: StateFlow<List<LauncherRowEntity>> = dao.getLauncherRowsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val menuItems: StateFlow<List<MenuItemEntity>> = dao.getMenuItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val services: StateFlow<List<ServiceEntity>> = dao.getServicesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val whitelistedApps: StateFlow<List<InstalledAppItem>> = repository.observeWhitelistedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allInstalledApps: StateFlow<List<InstalledAppItem>> = repository.observeInstalledApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recommendationRows: StateFlow<List<HomeFeedRow>> = repository.observeRecommendationRows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live TV row from Avex TV ContentProvider
    private val liveTvRow = MutableStateFlow<HomeFeedRow?>(null)

    private val guestPreferences: StateFlow<GuestLauncherPreferences> = guestPersonalizationManager.observeState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GuestLauncherPreferences())

    private val sourceItems = MutableStateFlow<List<SourceItem>>(emptyList())
    private val selectedDestination = MutableStateFlow(LauncherDestination.HOME)

    /** Ticks every 60 seconds so the meal period row updates without a restart */
    private val mealTick = MutableStateFlow(currentMealPeriod())

    private val adminConfig: StateFlow<AdminPanelConfigResponse> = adminPanelRepository.config
    private val bindingState = adminPanelRepository.bindingState

    val uiState: StateFlow<LauncherUiState> = combine(
        combine(
            roomInfo,
            rows,
            menuItems,
            adminConfig
        ) { roomInfo, rows, menuItems, adminConfig ->
            UiContentSnapshot(roomInfo, rows, menuItems, adminConfig)
        },
        combine(services, mealTick) { svc, meal -> svc to meal },
        combine(selectedDestination, guestPreferences, whitelistedApps, allInstalledApps, sourceItems) {
                destination,
                preferences,
                installedApps,
                allApps,
                sourceItems ->
            DeviceUiSnapshot(
                selectedDestination = destination,
                guestPreferences = preferences,
                installedApps = installedApps,
                allInstalledApps = allApps,
                sourceItems = sourceItems
            )
        },
        bindingState
    ) { contentSnapshot, (serviceEntities, mealPeriod), deviceUi, binding ->
        buildUiState(
            roomInfo = contentSnapshot.roomInfo,
            rows = contentSnapshot.rows,
            menuItems = contentSnapshot.menuItems,
            adminConfig = contentSnapshot.adminConfig,
            services = serviceEntities,
            mealPeriod = mealPeriod,
            selectedDestination = deviceUi.selectedDestination,
            guestPreferences = deviceUi.guestPreferences,
            installedApps = deviceUi.installedApps,
            allInstalledApps = deviceUi.allInstalledApps,
            sourceItems = deviceUi.sourceItems,
            bindingState = binding
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherUiState())

    init {
        refreshSourceItems()
        startMealTicker()
        startAdminPolling()
        fetchLiveTvChannels()
    }

    private fun startAdminPolling() {
        viewModelScope.launch {
            adminPanelRepository.startPolling()
        }
    }

    /**
     * Fetch Live TV channels from Avex TV ContentProvider.
     * Falls back silently if Avex TV is not installed.
     */
    private fun fetchLiveTvChannels() {
        viewModelScope.launch {
            try {
                val tvRow = repository.fetchLiveTvRow()
                liveTvRow.value = tvRow
            } catch (_: Exception) {
                liveTvRow.value = null
            }
        }
    }

    private fun startMealTicker() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L) // update every minute
                mealTick.value = currentMealPeriod()
            }
        }
    }

    fun onIntentAction(action: String?) {
        selectedDestination.value = LauncherDestination.fromAction(action)
    }

    fun onDestinationSelected(destination: LauncherDestination) {
        selectedDestination.value = destination
    }

    fun saveInstalledAppOrder(packageOrder: List<String>) {
        guestPersonalizationManager.saveAppOrder(packageOrder)
    }

    fun resetGuestPersonalization() {
        guestPersonalizationManager.clear()
    }

    private fun refreshSourceItems() {
        viewModelScope.launch {
            sourceItems.value = repository.getSourceItems()
        }
    }

    private fun buildUiState(
        roomInfo: RoomInfoEntity?,
        rows: List<LauncherRowEntity>,
        menuItems: List<MenuItemEntity>,
        adminConfig: AdminPanelConfigResponse,
        services: List<ServiceEntity>,
        mealPeriod: MealPeriod,
        selectedDestination: LauncherDestination,
        guestPreferences: GuestLauncherPreferences,
        installedApps: List<InstalledAppItem>,
        allInstalledApps: List<InstalledAppItem>,
        sourceItems: List<SourceItem>,
        bindingState: com.hotelvision.launcher.data.repository.BindingState =
            com.hotelvision.launcher.data.repository.BindingState.Loading
    ): LauncherUiState {
        val branding = HotelBranding(
            hotelName = adminConfig.hotel.name,
            shortBrand = adminConfig.hotel.name.take(2).uppercase(),
            tagline = adminConfig.hotel.tagline ?: "Welcome to our property",
            location = "Hotel Vision Premium"
        )
        
        // Use whitelisted apps for VIP row
        val displayApps = applyGuestAppOrder(installedApps, guestPreferences.appOrder)
        val guestMessages = buildGuestMessages(roomInfo, mealPeriod)

        val displaySources = sourceItems.ifEmpty { fallbackSourceItems() }
        val appsTitle = "Apps on This TV"

        val homeFeedRows = buildHomeFeedRows(
            mealPeriod = mealPeriod,
            menuItems = menuItems,
            services = services,
            displayApps = displayApps,
            sourceItems = displaySources,
            appsTitle = appsTitle,
            guestMessages = guestMessages,
            liveTvRow = liveTvRow.value
        )
        val roomServiceFeedRows = buildRoomServiceRows(
            mealPeriod = mealPeriod,
            menuItems = menuItems,
            services = services
        )
        val localGuideFeedRows = buildLocalGuideRows()

        val feedRows = when (selectedDestination) {
            LauncherDestination.HOME -> homeFeedRows
            LauncherDestination.ROOM_SERVICE -> roomServiceFeedRows
            LauncherDestination.LOCAL_GUIDE -> localGuideFeedRows
        }

        val destinationBackdrops = buildDestinationBackdrops(
            branding = branding,
            adminConfig = adminConfig,
            homeRows = homeFeedRows,
            roomServiceRows = roomServiceFeedRows,
            localGuideRows = localGuideFeedRows
        )

        val guestName = roomInfo?.guestName ?: "Guest"
        val roomLabel = roomInfo?.roomNumber?.let { "Room $it" } ?: "Suite Assigned"

        return LauncherUiState(
            selectedDestination = selectedDestination,
            hotelBranding = branding,
            roomInfo = roomInfo,
            adminConfig = adminConfig,
            welcomeTitle = "Welcome, $guestName",
            welcomeSubtitle = "$roomLabel | ${branding.hotelName}",
            mealPeriod = mealPeriod,
            ambientBackdrop = destinationBackdrops.forDestination(selectedDestination),
            destinationBackdrops = destinationBackdrops,
            feedRows = feedRows,
            guestMessages = guestMessages,
            simpleModeEnabled = false,
            allInstalledApps = allInstalledApps,
            bindingState = bindingState
        )
    }

    private fun buildDestinationBackdrops(
        branding: HotelBranding,
        adminConfig: AdminPanelConfigResponse,
        homeRows: List<HomeFeedRow>,
        roomServiceRows: List<HomeFeedRow>,
        localGuideRows: List<HomeFeedRow>
    ): DestinationBackdropsState {
        val hotelFallbackImage = adminConfig.hotel.heroImageUrl
            ?.takeIf { it.isNotBlank() }
            ?: MockHotelContent.defaultBackdrop.imageUrl
        val hotelSlideshow = adminConfig.screensaver.assets
            .filter { it.isNotBlank() }
            .ifEmpty { MockHotelContent.defaultBackdrop.slideshowImages }

        val homeImages = extractBackdropImages(homeRows)
        val roomServiceImages = extractBackdropImages(roomServiceRows)
        val localGuideImages = extractBackdropImages(localGuideRows)

        val homeBackdrop = AmbientBackdropState(
            title = branding.hotelName,
            imageUrl = hotelFallbackImage,
            slideshowImages = (hotelSlideshow + homeImages).distinct()
        )
        return DestinationBackdropsState(
            home = homeBackdrop,
            roomService = buildDestinationBackdrop(
                title = "Room Service",
                fallbackImage = roomServiceImages.firstOrNull() ?: hotelFallbackImage,
                slideshowImages = roomServiceImages.ifEmpty { homeBackdrop.slideshowImages }
            ),
            localGuide = buildDestinationBackdrop(
                title = "Local Guide",
                fallbackImage = localGuideImages.firstOrNull() ?: hotelFallbackImage,
                slideshowImages = localGuideImages.ifEmpty { homeBackdrop.slideshowImages }
            )
        )
    }

    private fun buildDestinationBackdrop(
        title: String,
        fallbackImage: String,
        slideshowImages: List<String>
    ): AmbientBackdropState {
        return AmbientBackdropState(
            title = title,
            imageUrl = fallbackImage,
            slideshowImages = slideshowImages.distinct()
        )
    }

    private fun extractBackdropImages(rows: List<HomeFeedRow>): List<String> {
        return rows
            .flatMap { row ->
                row.cards.mapNotNull { card ->
                    card.ambientImageUrl?.takeIf { it.isNotBlank() }
                        ?: card.imageUrl?.takeIf { it.isNotBlank() }
                }
            }
            .distinct()
    }

    /**
     * Build Home Feed Rows with new hierarchy:
     * Row 1: "Live TV" (16:9 Leanback Cards)
     * Row 2: "Dining & Services" (16:9 Leanback Cards)
     * Row 3: "Apps on this TV" (1:1 Circular Icons) + "All Apps" button
     */
    private fun buildHomeFeedRows(
        mealPeriod: MealPeriod,
        menuItems: List<MenuItemEntity>,
        services: List<ServiceEntity>,
        displayApps: List<InstalledAppItem>,
        sourceItems: List<SourceItem>,
        appsTitle: String,
        guestMessages: List<GuestMessageItem>,
        liveTvRow: HomeFeedRow?
    ): List<HomeFeedRow> {
        val conciergeFallbackSection = MockHotelContent.hotelFeatureSections()
            .first { it.id == "services" }
        return buildList {
            // Row 1: Live TV (from Avex TV ContentProvider)
            liveTvRow?.let { add(it) }
            
            // Row 2: Dining (time-aware food menu)
            buildDiningRow(mealPeriod, menuItems)?.let { add(it) }
            
            // Row 3: Apps on this TV (whitelisted only) + All Apps button
            if (displayApps.isNotEmpty()) {
                val appsWithAllApps = displayApps + InstalledAppItem(
                    id = "all_apps",
                    packageName = "all_apps",
                    launchActivityClassName = null,
                    title = "All Apps",
                    subtitle = "See all installed apps",
                    badge = "ALL",
                    isSystemApp = false,
                    action = LauncherAction.OpenAllApps
                )
                add(
                    HomeFeedRow(
                        id = "apps_installed",
                        title = appsTitle,
                        subtitle = "",
                        rowType = HomeFeedRowType.APPS_INSTALLED,
                        style = HomeSectionStyle.COMPACT,
                        cards = appsWithAllApps.map(InstalledAppItem::toInstalledAppHomeCard)
                    )
                )
            }
            
            // Row 4: Dining & Services
            add(
                buildServicesRow(
                    title = "Dining & Services",
                    services = services,
                    fallbackSection = conciergeFallbackSection
                )
            )
        }
    }

    private fun buildRoomServiceRows(
        mealPeriod: MealPeriod,
        menuItems: List<MenuItemEntity>,
        services: List<ServiceEntity>
    ): List<HomeFeedRow> {
        val mealForRoomService = if (mealPeriod == MealPeriod.OFF_HOURS) {
            MealPeriod.DINNER
        } else {
            mealPeriod
        }
        val serviceFallbackSection = MockHotelContent.hotelFeatureSections()
            .first { it.id == "services" }

        return buildList {
            buildDiningRow(mealForRoomService, menuItems)?.let { add(it) }
            add(
                buildServicesRow(
                    title = "Room Service",
                    services = services,
                    fallbackSection = serviceFallbackSection
                )
            )
        }
    }

    private fun buildLocalGuideRows(): List<HomeFeedRow> {
        return MockHotelContent.hotelFeatureSections()
            .filter { section -> section.id in setOf("experiences", "offers") }
            .map { section ->
                mapFeatureSection(
                    section = section,
                    rowType = HomeFeedRowType.HOTEL_FEATURES,
                    cardType = LauncherCardType.FEATURED
                )
            }
    }

    private fun buildLocalGuidePreviewRows(): List<HomeFeedRow> {
        return MockHotelContent.hotelFeatureSections()
            .filter { section -> section.id in setOf("experiences", "offers", "spa") }
            .map { section ->
                mapFeatureSection(
                    section = section,
                    rowType = HomeFeedRowType.HOTEL_FEATURES,
                    cardType = LauncherCardType.FEATURED
                )
            }
    }

    private fun buildDiningRow(
        mealPeriod: MealPeriod,
        menuItems: List<MenuItemEntity>
    ): HomeFeedRow? {
        // Returns null during OFF_HOURS — row is simply omitted from the feed
        val fallbackSection = MockHotelContent.diningRecommendations(mealPeriod) ?: return null

        if (menuItems.isEmpty()) {
            return mapFeatureSection(
                section = fallbackSection,
                rowType = HomeFeedRowType.DINING_TIME_AWARE,
                cardType = LauncherCardType.DINING
            )
        }

        return HomeFeedRow(
            id = "dining_time_aware",
            title = mealRowTitle(mealPeriod),
            subtitle = mealRowSubtitle(mealPeriod),
            rowType = HomeFeedRowType.DINING_TIME_AWARE,
            style = HomeSectionStyle.STANDARD,
            cards = menuItems.take(6).map { item ->
                HomeCard(
                    id = "menu_${item.id}",
                    title = item.name,
                    subtitle = item.category,
                    supportingText = "Available now | ${item.price.toInt()} BDT",
                    imageUrl = item.imageUrl,
                    ambientImageUrl = item.imageUrl,
                    sourceLabel = brandingLabel(),
                    badge = badgeFrom(item.category),
                    accentColor = 0xFFCF8E38,
                    cardType = LauncherCardType.DINING,
                    action = LauncherAction.None
                )
            }
        )
    }

    private fun buildServicesRow(
        title: String,
        services: List<ServiceEntity>,
        fallbackSection: HotelFeatureSection
    ): HomeFeedRow {
        if (services.isEmpty()) {
            return mapFeatureSection(
                section = fallbackSection.copy(title = title),
                rowType = HomeFeedRowType.HOTEL_FEATURES,
                cardType = LauncherCardType.SERVICE
            )
        }

        return HomeFeedRow(
            id = "services",
            title = title,
            subtitle = "Services enabled for ${MockHotelContent.branding.hotelName}",
            rowType = HomeFeedRowType.HOTEL_FEATURES,
            style = HomeSectionStyle.COMPACT,
            cards = services.map { service ->
                HomeCard(
                    id = "service_${service.id}",
                    title = service.name,
                    subtitle = "Guest assistance",
                    supportingText = "Configured for this property through the admin panel.",
                    sourceLabel = MockHotelContent.branding.shortBrand,
                    badge = service.icon.ifBlank { badgeFrom(service.name) },
                    accentColor = 0xFF2C6E80,
                    cardType = LauncherCardType.SERVICE,
                    action = LauncherAction.None
                )
            }
        )
    }

    private fun buildGuestMessages(roomInfo: RoomInfoEntity?, mealPeriod: MealPeriod): List<GuestMessageItem> {
        val guestName = roomInfo?.guestName ?: "Guest"
        val mealLabel = when (mealPeriod) {
            MealPeriod.BREAKFAST -> "Breakfast service is open for your suite."
            MealPeriod.LUNCH -> "Lunch service is now available with poolside and in-room options."
            MealPeriod.DINNER -> "Dinner service is open with rooftop and lounge seating tonight."
            MealPeriod.LATE_NIGHT -> "Late night cravings? Our midnight menu is available for a limited time."
            MealPeriod.OFF_HOURS -> "Our overnight guest services remain available for anything you need."
        }

        return listOf(
            GuestMessageItem(
                id = "welcome_privacy",
                title = "Welcome, $guestName",
                body = "Your stay preferences are private and will reset automatically at checkout.",
                timestampLabel = "Now",
                badge = "VIP",
                action = LauncherAction.None
            ),
            GuestMessageItem(
                id = "concierge_ready",
                title = "Concierge Is Ready",
                body = "Butler, transport, spa, and dining requests are available with one click.",
                timestampLabel = "Concierge",
                badge = "CARE",
                action = LauncherAction.OpenDestination(LauncherDestination.ROOM_SERVICE)
            ),
            GuestMessageItem(
                id = "dining_update",
                title = "Dining Update",
                body = mealLabel,
                timestampLabel = "Dining",
                badge = "DIN",
                action = LauncherAction.OpenDestination(LauncherDestination.ROOM_SERVICE)
            ),
            GuestMessageItem(
                id = "local_guide",
                title = "Curated Local Guide",
                body = "Luxury shopping, city transport, and executive lounge experiences are ready.",
                timestampLabel = "Local Guide",
                badge = "CITY",
                action = LauncherAction.OpenDestination(LauncherDestination.LOCAL_GUIDE)
            )
        )
    }

    private fun mapFeatureSection(
        section: HotelFeatureSection,
        rowType: HomeFeedRowType,
        cardType: LauncherCardType
    ): HomeFeedRow {
        return HomeFeedRow(
            id = section.id,
            title = section.title,
            subtitle = section.subtitle,
            rowType = rowType,
            style = section.style,
            cards = section.cards.map { feature ->
                HomeCard(
                    id = feature.id,
                    title = feature.title,
                    subtitle = feature.subtitle,
                    supportingText = feature.supportingText,
                    imageUrl = feature.imageUrl,
                    ambientImageUrl = feature.ambientImageUrl ?: feature.imageUrl,
                    sourceLabel = MockHotelContent.branding.shortBrand,
                    badge = feature.badge,
                    accentColor = feature.accentColor,
                    cardType = cardType,
                    action = feature.action
                )
            }
        )
    }

    private fun currentMealPeriod(): MealPeriod {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 6..10  -> MealPeriod.BREAKFAST   // 06:00 – 10:59
            in 11..14 -> MealPeriod.LUNCH        // 11:00 – 14:59
            in 18..21 -> MealPeriod.DINNER       // 18:00 – 21:59
            in 22..23 -> MealPeriod.LATE_NIGHT    // 22:00 – 23:59
            else      -> MealPeriod.OFF_HOURS    // food row hidden
        }
    }

    private fun fallbackSourceItems(): List<SourceItem> {
        return listOf(
            SourceItem(
                id = "source_settings",
                title = "Source Settings",
                subtitle = "Open TV device settings",
                badge = "SET",
                isSystemProvided = false,
                action = LauncherAction.OpenControlPanel
            )
        )
    }

    private fun badgeFrom(label: String): String {
        return label
            .split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { label.take(2).uppercase() }
            .take(3)
    }

    private fun brandingLabel(): String = MockHotelContent.branding.shortBrand

    private fun mealRowTitle(mealPeriod: MealPeriod): String = when (mealPeriod) {
        MealPeriod.BREAKFAST -> "Breakfast Menu"
        MealPeriod.LUNCH -> "Lunch Menu"
        MealPeriod.DINNER -> "Dinner Menu"
        MealPeriod.LATE_NIGHT -> "Late Night Menu"
        MealPeriod.OFF_HOURS -> "Dining"
    }

    private fun mealRowSubtitle(mealPeriod: MealPeriod): String = when (mealPeriod) {
        MealPeriod.BREAKFAST -> "Morning dining curated for guests starting the day | 06:00 - 11:00"
        MealPeriod.LUNCH -> "Midday favourites for business and leisure stays | 11:00 - 15:00"
        MealPeriod.DINNER -> "Evening dining and lounge experiences for tonight | 18:00 - 22:00"
        MealPeriod.LATE_NIGHT -> "Supper and midnight snacks for night owls | 22:00 - 00:00"
        MealPeriod.OFF_HOURS -> "In-room and concierge dining remain available for this stay."
    }

    private fun applyGuestAppOrder(
        installedApps: List<InstalledAppItem>,
        packageOrder: List<String>
    ): List<InstalledAppItem> {
        if (installedApps.isEmpty() || packageOrder.isEmpty()) return installedApps

        val appsByPackage = installedApps.associateBy { it.packageName }
        val ordered = packageOrder.mapNotNull(appsByPackage::get)
        val remainder = installedApps.filterNot { it.packageName in packageOrder.toSet() }
        return ordered + remainder
    }

    private data class UiContentSnapshot(
        val roomInfo: RoomInfoEntity?,
        val rows: List<LauncherRowEntity>,
        val menuItems: List<MenuItemEntity>,
        val adminConfig: AdminPanelConfigResponse
    )

    private data class DeviceUiSnapshot(
        val selectedDestination: LauncherDestination,
        val guestPreferences: GuestLauncherPreferences,
        val installedApps: List<InstalledAppItem>,
        val allInstalledApps: List<InstalledAppItem>,
        val sourceItems: List<SourceItem>
    )
}
