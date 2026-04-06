package com.hotelvision.launcher.di

import com.hotelvision.launcher.data.device.AndroidAppsProvider
import com.hotelvision.launcher.data.device.AndroidInputsProvider
import com.hotelvision.launcher.data.device.AndroidTvRecommendationsProvider
import com.hotelvision.launcher.data.device.AppsProvider
import com.hotelvision.launcher.data.device.InputsProvider
import com.hotelvision.launcher.data.device.RecommendationsProvider
import com.hotelvision.launcher.data.device.WhitelistAppsProvider
import com.hotelvision.launcher.data.device.WhitelistAppsProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceIntegrationModule {

    @Binds
    @Singleton
    abstract fun bindAppsProvider(
        provider: AndroidAppsProvider
    ): AppsProvider

    @Binds
    @Singleton
    abstract fun bindWhitelistAppsProvider(
        provider: WhitelistAppsProviderImpl
    ): WhitelistAppsProvider

    @Binds
    @Singleton
    abstract fun bindInputsProvider(
        provider: AndroidInputsProvider
    ): InputsProvider

    @Binds
    @Singleton
    abstract fun bindRecommendationsProvider(
        provider: AndroidTvRecommendationsProvider
    ): RecommendationsProvider
}
