package com.hotelvision.launcher.data.repository

import com.hotelvision.launcher.data.api.LauncherApiService
import com.hotelvision.launcher.data.db.dao.LauncherDao
import com.hotelvision.launcher.data.db.entities.RoomInfoEntity
import com.hotelvision.launcher.data.device.AppsProvider
import com.hotelvision.launcher.data.device.InputsProvider
import com.hotelvision.launcher.data.device.RecommendationsProvider
import com.hotelvision.launcher.data.device.WhitelistAppsProvider
import com.hotelvision.launcher.ui.HomeFeedRow
import com.hotelvision.launcher.ui.InstalledAppItem
import com.hotelvision.launcher.ui.SourceItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class LauncherRepository @Inject constructor(
    private val api: LauncherApiService,
    private val dao: LauncherDao,
    private val appsProvider: AppsProvider,
    private val whitelistAppsProvider: WhitelistAppsProvider,
    private val inputsProvider: InputsProvider,
    private val recommendationsProvider: RecommendationsProvider,
    private val liveTvRepository: LiveTvRepository
) {
    fun getRoomInfo(): Flow<RoomInfoEntity?> = dao.getRoomInfoFlow()

    suspend fun getInstalledApps(): List<InstalledAppItem> = appsProvider.getInstalledApps()

    fun observeInstalledApps(): Flow<List<InstalledAppItem>> = appsProvider.observeInstalledApps()

    suspend fun getWhitelistedApps(): List<InstalledAppItem> = whitelistAppsProvider.getWhitelistedApps()

    fun observeWhitelistedApps(): Flow<List<InstalledAppItem>> = whitelistAppsProvider.observeWhitelistedApps()

    fun observeRecommendationRows(): Flow<List<HomeFeedRow>> = recommendationsProvider.observeRecommendationRows()

    suspend fun getSourceItems(): List<SourceItem> = inputsProvider.getSourceItems()

    suspend fun fetchLiveTvRow(): HomeFeedRow? = liveTvRepository.fetchLiveTvRow()

    suspend fun syncRoomInfo(roomNum: String) {
        try {
            val response = api.getRoomInfo(roomNum)
            val entity = RoomInfoEntity(
                roomNumber = response.roomNumber,
                guestName = response.guestName,
                checkoutTime = response.checkoutTime,
                status = response.status
            )
            dao.insertRoomInfo(entity)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
}
