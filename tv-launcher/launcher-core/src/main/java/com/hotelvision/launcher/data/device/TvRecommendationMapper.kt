package com.hotelvision.launcher.data.device

import com.hotelvision.launcher.ui.HomeCard
import com.hotelvision.launcher.ui.HomeFeedRow
import com.hotelvision.launcher.ui.HomeFeedRowType
import com.hotelvision.launcher.ui.HomeSectionStyle
import com.hotelvision.launcher.ui.LauncherAction
import com.hotelvision.launcher.ui.LauncherCardType

internal data class RecommendationChannelRecord(
    val channelId: Long,
    val packageName: String?,
    val title: String,
    val description: String,
    val appLabel: String,
    val badge: String,
    val accentColor: Long = 0xFF365BDE
)

internal data class RecommendationProgramRecord(
    val programId: Long,
    val channelId: Long,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val intentUri: String?
)

internal object TvRecommendationMapper {

    fun mapRows(
        channels: List<RecommendationChannelRecord>,
        programsByChannelId: Map<Long, List<RecommendationProgramRecord>>,
        resolveAction: (intentUri: String?, packageName: String?) -> LauncherAction,
        maxRows: Int = 4,
        maxCardsPerRow: Int = 6
    ): List<HomeFeedRow> {
        return channels
            .take(maxRows)
            .mapNotNull { channel ->
                val cards = programsByChannelId[channel.channelId]
                    .orEmpty()
                    .take(maxCardsPerRow)
                    .map { program ->
                        HomeCard(
                            id = "preview_program_${program.programId}",
                            title = program.title,
                            subtitle = channel.appLabel,
                            supportingText = program.description,
                            imageUrl = program.imageUrl,
                            ambientImageUrl = program.imageUrl,
                            sourceLabel = channel.appLabel,
                            packageName = channel.packageName,
                            badge = channel.badge,
                            accentColor = channel.accentColor,
                            cardType = LauncherCardType.RECOMMENDATION,
                            action = resolveAction(program.intentUri, channel.packageName)
                        )
                    }

                if (cards.isEmpty()) {
                    null
                } else {
                    HomeFeedRow(
                        id = "preview_channel_${channel.channelId}",
                        title = channel.title.ifBlank { channel.appLabel },
                        subtitle = channel.description.ifBlank { "Recommended from ${channel.appLabel}" },
                        rowType = HomeFeedRowType.APP_RECOMMENDATIONS,
                        style = HomeSectionStyle.STANDARD,
                        cards = cards
                    )
                }
            }
    }
}

internal object TvRecommendationActionResolver {

    fun resolve(
        intentUri: String?,
        packageName: String?,
        parseIntentUri: (String) -> LauncherAction?
    ): LauncherAction {
        if (!intentUri.isNullOrBlank()) {
            parseIntentUri(intentUri)?.let { return it }
        }

        return packageName
            ?.takeIf { it.isNotBlank() }
            ?.let(LauncherAction::LaunchPackage)
            ?: LauncherAction.None
    }
}
