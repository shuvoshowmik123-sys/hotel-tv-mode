package com.hotelvision.launcher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hotelvision.launcher.data.db.dao.LauncherDao
import com.hotelvision.launcher.data.db.entities.*

@Database(
    entities = [
        RoomInfoEntity::class,
        LauncherRowEntity::class,
        AppConfigEntity::class,
        MenuItemEntity::class,
        ServiceEntity::class,
        SyncMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao
}
