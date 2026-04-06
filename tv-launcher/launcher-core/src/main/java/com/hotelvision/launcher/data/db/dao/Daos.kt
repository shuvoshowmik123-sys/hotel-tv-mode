package com.hotelvision.launcher.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hotelvision.launcher.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    @Query("SELECT * FROM room_info WHERE id = 1")
    fun getRoomInfoFlow(): Flow<RoomInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomInfo(roomInfo: RoomInfoEntity)

    @Query("SELECT * FROM launcher_rows WHERE isVisible = 1 ORDER BY sortOrder ASC")
    fun getLauncherRowsFlow(): Flow<List<LauncherRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLauncherRows(rows: List<LauncherRowEntity>)

    @Query("SELECT * FROM apps_config WHERE rowId = :rowId AND isVisible = 1 ORDER BY sortOrder ASC")
    fun getAppsForRowFlow(rowId: Int): Flow<List<AppConfigEntity>>

    @Query("SELECT * FROM apps_config WHERE isVisible = 1 ORDER BY rowId ASC, sortOrder ASC")
    fun getAllAppsFlow(): Flow<List<AppConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppConfigEntity>)
    
    @Query("SELECT * FROM menu_items WHERE isAvailable = 1 ORDER BY sortOrder ASC")
    fun getMenuItemsFlow(): Flow<List<MenuItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Query("SELECT * FROM services")
    fun getServicesFlow(): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceEntity>)
}
