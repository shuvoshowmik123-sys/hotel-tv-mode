package com.hotelvision.launcher.data.device

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.tv.TvContract
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.util.Log
import com.hotelvision.launcher.ui.HomeFeedRow
import com.hotelvision.launcher.ui.LauncherAction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

interface RecommendationsProvider {
    fun observeRecommendationRows(): Flow<List<HomeFeedRow>>
}

@Singleton
class AndroidTvRecommendationsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : RecommendationsProvider {

    private val loggedProviderFailure = AtomicBoolean(false)

    override fun observeRecommendationRows(): Flow<List<HomeFeedRow>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !hasTvProvider()) {
            return flowOf(emptyList())
        }

        return callbackFlow {
            val resolver = context.contentResolver
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    refreshRows()
                }

                override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                    refreshRows()
                }

                private fun refreshRows() {
                    launch(Dispatchers.IO) {
                        trySend(loadRecommendationRows(resolver))
                    }
                }
            }

            try {
                resolver.registerContentObserver(TvContract.Channels.CONTENT_URI, true, observer)
                resolver.registerContentObserver(TvContract.PreviewPrograms.CONTENT_URI, true, observer)
            } catch (securityException: SecurityException) {
                logProviderFailureOnce(securityException)
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            launch(Dispatchers.IO) {
                trySend(loadRecommendationRows(resolver))
            }

            awaitClose {
                resolver.unregisterContentObserver(observer)
            }
        }.distinctUntilChanged()
    }

    private fun loadRecommendationRows(resolver: ContentResolver): List<HomeFeedRow> {
        return runCatching {
            val channels = queryPreviewChannels(resolver)
            if (channels.isEmpty()) return emptyList()

            val limitedChannels = channels.take(4)
            val programsByChannelId = limitedChannels.associate { channel ->
                channel.channelId to queryPreviewPrograms(resolver, channel.channelId)
            }

            TvRecommendationMapper.mapRows(
                channels = limitedChannels,
                programsByChannelId = programsByChannelId,
                resolveAction = ::resolveRecommendationAction
            )
        }.getOrElse { throwable ->
            logProviderFailureOnce(throwable)
            emptyList()
        }
    }

    private fun hasTvProvider(): Boolean {
        return runCatching {
            context.packageManager.resolveContentProvider(TvContract.AUTHORITY, PackageManager.MATCH_SYSTEM_ONLY) != null
                || context.packageManager.resolveContentProvider(TvContract.AUTHORITY, 0) != null
        }.getOrDefault(false)
    }

    private fun queryPreviewChannels(resolver: ContentResolver): List<RecommendationChannelRecord> {
        val projection = arrayOf(
            BaseColumns._ID,
            TvContract.Channels.COLUMN_PACKAGE_NAME,
            TvContract.Channels.COLUMN_DISPLAY_NAME,
            TvContract.Channels.COLUMN_DESCRIPTION
        )

        val selection = "${TvContract.Channels.COLUMN_BROWSABLE} = ? AND ${TvContract.Channels.COLUMN_TYPE} = ?"
        val selectionArgs = arrayOf("1", TvContract.Channels.TYPE_PREVIEW)

        return resolver.query(
            TvContract.Channels.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val packageIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_PACKAGE_NAME)
            val titleIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DISPLAY_NAME)
            val descriptionIndex = cursor.getColumnIndexOrThrow(TvContract.Channels.COLUMN_DESCRIPTION)

            buildList {
                while (cursor.moveToNext()) {
                    val packageName = cursor.getString(packageIndex)?.takeIf { it.isNotBlank() }
                    val appLabel = loadAppLabel(packageName)
                    val channelTitle = cursor.getString(titleIndex)?.trim().orEmpty()
                    add(
                        RecommendationChannelRecord(
                            channelId = cursor.getLong(idIndex),
                            packageName = packageName,
                            title = channelTitle.ifBlank { appLabel },
                            description = cursor.getString(descriptionIndex)?.trim().orEmpty(),
                            appLabel = appLabel,
                            badge = badgeFrom(appLabel)
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun queryPreviewPrograms(
        resolver: ContentResolver,
        channelId: Long
    ): List<RecommendationProgramRecord> {
        val projection = arrayOf(
            BaseColumns._ID,
            TvContract.PreviewPrograms.COLUMN_CHANNEL_ID,
            TvContract.PreviewPrograms.COLUMN_TITLE,
            TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.PreviewPrograms.COLUMN_INTENT_URI
        )

        return resolver.query(
            TvContract.PreviewPrograms.CONTENT_URI,
            projection,
            "${TvContract.PreviewPrograms.COLUMN_CHANNEL_ID} = ?",
            arrayOf(channelId.toString()),
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val channelIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_CHANNEL_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_SHORT_DESCRIPTION)
            val posterIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI)
            val thumbnailIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI)
            val intentIndex = cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_INTENT_URI)

            buildList {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleIndex)?.trim().orEmpty()
                    if (title.isBlank()) continue

                    add(
                        RecommendationProgramRecord(
                            programId = cursor.getLong(idIndex),
                            channelId = cursor.getLong(channelIndex),
                            title = title,
                            description = cursor.getString(descriptionIndex)?.trim().orEmpty(),
                            imageUrl = cursor.getString(posterIndex)?.takeIf { it.isNotBlank() }
                                ?: cursor.getString(thumbnailIndex)?.takeIf { it.isNotBlank() },
                            intentUri = cursor.getString(intentIndex)?.takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private fun resolveRecommendationAction(intentUri: String?, packageName: String?): LauncherAction {
        return TvRecommendationActionResolver.resolve(intentUri, packageName) { uri ->
            runCatching {
                LauncherAction.LaunchIntent(Intent.parseUri(uri, Intent.URI_INTENT_SCHEME))
            }.getOrNull()
        }
    }

    private fun loadAppLabel(packageName: String?): String {
        if (packageName.isNullOrBlank()) return "Recommended"

        return runCatching {
            val packageManager = context.packageManager
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
                .trim()
        }.getOrDefault(packageName)
    }

    private fun badgeFrom(label: String): String {
        return label
            .split(" ")
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { label.take(2).uppercase(Locale.getDefault()) }
            .take(3)
    }

    private fun logProviderFailureOnce(throwable: Throwable) {
        if (loggedProviderFailure.compareAndSet(false, true)) {
            Log.w(TAG, "Unable to load TV provider recommendations.", throwable)
        }
    }

    private companion object {
        const val TAG = "TvRecommendations"
    }
}
