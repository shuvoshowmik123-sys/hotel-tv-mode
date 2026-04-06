package com.hotelvision.launcher.di

import android.content.Context
import androidx.room.Room
import com.hotelvision.launcher.data.db.LauncherDatabase
import com.hotelvision.launcher.data.db.MIGRATION_1_2
import com.hotelvision.launcher.data.db.dao.LauncherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLauncherDatabase(@ApplicationContext context: Context): LauncherDatabase {
        return Room.databaseBuilder(
            context,
            LauncherDatabase::class.java,
            "launcher.db"
        )
            .addMigrations(MIGRATION_1_2)
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    @Singleton
    fun provideLauncherDao(database: LauncherDatabase): LauncherDao = database.launcherDao()
}
