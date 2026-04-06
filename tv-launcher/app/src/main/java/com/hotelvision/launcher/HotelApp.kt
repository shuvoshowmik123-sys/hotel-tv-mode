package com.hotelvision.launcher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import com.hotelvision.launcher.data.session.SessionResetWorker
import com.hotelvision.launcher.performance.LauncherPerformanceProfile
import com.hotelvision.launcher.performance.resolveLauncherPerformanceProfile
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class HotelApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val performanceProfile by lazy { resolveLauncherPerformanceProfile(this) }

    override fun onCreate() {
        super.onCreate()
        scheduleSessionReset()
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        val memoryCacheBytes = if (performanceProfile == LauncherPerformanceProfile.LOW_RAM) {
            8 * 1024 * 1024
        } else {
            24 * 1024 * 1024
        }

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(memoryCacheBytes.toLong())
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.toOkioPath().resolve("coil_cache"))
                    .maxSizeBytes(64 * 1024 * 1024L)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    private fun scheduleSessionReset() {
        try {
            val resetRequest = PeriodicWorkRequestBuilder<SessionResetWorker>(
                24,
                TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(this).enqueue(resetRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
