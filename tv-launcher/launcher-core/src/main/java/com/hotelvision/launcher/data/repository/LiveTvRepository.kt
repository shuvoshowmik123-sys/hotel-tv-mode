package com.hotelvision.launcher.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hotelvision.launcher.ui.HomeCard
import com.hotelvision.launcher.ui.HomeFeedRow
import com.hotelvision.launcher.ui.HomeFeedRowType
import com.hotelvision.launcher.ui.HomeSectionStyle
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.LauncherCardType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live TV Repository - Queries Avex TV ContentProvider for live channels.
 * 
 * Memory Safety: Maximum 5 records, cursor closed in finally block.
 * Fallback: Silently hides Live TV row if Avex TV is not installed or provider fails.
 */
@Singleton
class LiveTvRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val CONTENT_URI = Uri.parse("content://com.avex.tv.provider/channels")
        private const val COLUMN_CHANNEL_NAME = "CHANNEL_NAME"
        private const val COLUMN_LOGO_URL = "LOGO_URL"
        private const val COLUMN_DEEP_LINK_URI = "DEEP_LINK_URI"
        private const val MAX_RESULTS = 5
    }

    /**
     * Fetches Live TV channels from Avex TV ContentProvider.
     * Returns null if Avex TV is not installed or provider fails.
     */
    suspend fun fetchLiveTvRow(): HomeFeedRow? = withContext(Dispatchers.IO) {
        try {
            val channels = queryChannelsProvider()
            if (channels.isEmpty()) return@withContext null

            val cards = channels.map { channel ->
                HomeCard(
                    id = "live_tv_${channel.id}",
                    title = channel.name,
                    subtitle = "Live TV",
                    supportingText = "",
                    imageUrl = channel.logoUrl,
                    ambientImageUrl = channel.logoUrl,
                    sourceLabel = "Avex TV",
                    badge = "TV",
                    accentColor = 0xFF6C5CE7,
                    cardType = LauncherCardType.RECOMMENDATION,
                    action = try {
                        LauncherAction.LaunchIntent(
                            Intent.parseUri(channel.deepLinkUri, Intent.URI_INTENT_SCHEME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (e: Exception) {
                        LauncherAction.None
                    }
                )
            }

            if (cards.isEmpty()) return@withContext null

            HomeFeedRow(
                id = "live_tv_channels",
                title = "Live TV",
                subtitle = "Watch live channels from your room",
                rowType = HomeFeedRowType.APP_RECOMMENDATIONS,
                style = HomeSectionStyle.STANDARD,
                cards = cards
            )
        } catch (e: Exception) {
            // Silently fail - hide Live TV row if Avex TV not installed or provider fails
            null
        }
    }

    /**
     * Queries Avex TV ContentProvider for channels.
     * Memory Safety: Max 5 records, cursor closed in finally block.
     */
    private fun queryChannelsProvider(): List<LiveTvChannel> {
        val channels = mutableListOf<LiveTvChannel>()
        var cursor: android.database.Cursor? = null

        try {
            cursor = context.contentResolver.query(
                CONTENT_URI,
                null,
                null,
                null,
                "LIMIT $MAX_RESULTS"
            )

            cursor?.let { c ->
                val nameIndex = c.getColumnIndex(COLUMN_CHANNEL_NAME)
                val logoIndex = c.getColumnIndex(COLUMN_LOGO_URL)
                val deepLinkIndex = c.getColumnIndex(COLUMN_DEEP_LINK_URI)

                while (c.moveToNext()) {
                    val name = if (nameIndex >= 0) c.getString(nameIndex)?.trim() else null
                    val logoUrl = if (logoIndex >= 0) c.getString(logoIndex)?.trim() else null
                    val deepLinkUri = if (deepLinkIndex >= 0) c.getString(deepLinkIndex)?.trim() else null

                    if (!name.isNullOrBlank() && !deepLinkUri.isNullOrBlank()) {
                        channels.add(
                            LiveTvChannel(
                                id = "channel_${channels.size}",
                                name = name,
                                logoUrl = logoUrl ?: "",
                                deepLinkUri = deepLinkUri
                            )
                        )
                    }
                }
            }
        } finally {
            // CRITICAL: Always close cursor to prevent memory leaks on 512MB hardware
            cursor?.close()
        }

        return channels.take(MAX_RESULTS)
    }
}

/**
 * Lightweight data class for Live TV channels.
 */
data class LiveTvChannel(
    val id: String,
    val name: String,
    val logoUrl: String,
    val deepLinkUri: String
)
