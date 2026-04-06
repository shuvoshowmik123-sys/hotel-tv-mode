package com.hotelvision.launcher.data.device

import com.hotelvision.launcher.ui.LauncherAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvRecommendationMapperTest {

    @Test
    fun mapRows_buildsRecommendationRowsFromChannelAndProgramData() {
        val rows = TvRecommendationMapper.mapRows(
            channels = listOf(
                RecommendationChannelRecord(
                    channelId = 10L,
                    packageName = "com.avex.tv",
                    title = "Avex Live",
                    description = "Live channels from Avex",
                    appLabel = "Avex TV",
                    badge = "ATV"
                )
            ),
            programsByChannelId = mapOf(
                10L to listOf(
                    RecommendationProgramRecord(
                        programId = 200L,
                        channelId = 10L,
                        title = "News Hour",
                        description = "Top headlines",
                        imageUrl = "https://example.com/news.png",
                        intentUri = "intent://news"
                    )
                )
            ),
            resolveAction = { _, packageName -> LauncherAction.LaunchPackage(packageName.orEmpty()) }
        )

        assertEquals(1, rows.size)
        assertEquals("Avex Live", rows.first().title)
        assertEquals("News Hour", rows.first().cards.first().title)
        assertEquals("Avex TV", rows.first().cards.first().sourceLabel)
    }

    @Test
    fun resolveAction_fallsBackToPackageWhenIntentIsInvalid() {
        val action = TvRecommendationActionResolver.resolve(
            intentUri = "bad://intent",
            packageName = "com.avex.tv",
            parseIntentUri = { null }
        )

        assertEquals(LauncherAction.LaunchPackage("com.avex.tv"), action)
    }

    @Test
    fun mapRows_skipsChannelsWithoutPrograms() {
        val rows = TvRecommendationMapper.mapRows(
            channels = listOf(
                RecommendationChannelRecord(
                    channelId = 11L,
                    packageName = "com.avex.tv",
                    title = "Empty",
                    description = "",
                    appLabel = "Avex TV",
                    badge = "ATV"
                )
            ),
            programsByChannelId = emptyMap(),
            resolveAction = { _, _ -> LauncherAction.None }
        )

        assertTrue(rows.isEmpty())
    }
}
