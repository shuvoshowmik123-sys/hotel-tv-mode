package com.hotelvision.launcher.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_info")
data class RoomInfoEntity(
    @PrimaryKey val id: Int = 1,
    val roomNumber: String,
    val guestName: String?,
    val checkoutTime: String?,
    val status: String
)

@Entity(tableName = "launcher_rows")
data class LauncherRowEntity(
    @PrimaryKey val id: Int,
    val rowType: String,
    val title: String,
    val titleBn: String?,
    val sortOrder: Int,
    val isVisible: Boolean
)

@Entity(tableName = "apps_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int,
    val packageName: String,
    val label: String,
    val iconUrl: String?,
    val rowId: Int,
    val sortOrder: Int,
    val isVisible: Boolean
)

@Entity(tableName = "menu_items")
data class MenuItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val nameBn: String?,
    val price: Double,
    val category: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val isAvailable: Boolean
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val nameBn: String?,
    val icon: String
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int = 1,
    val lastSyncedAt: String,
    val serverVersion: Long
)
