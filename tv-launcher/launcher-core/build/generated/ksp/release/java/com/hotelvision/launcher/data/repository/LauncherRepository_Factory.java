package com.hotelvision.launcher.data.repository;

import com.hotelvision.launcher.data.api.LauncherApiService;
import com.hotelvision.launcher.data.db.dao.LauncherDao;
import com.hotelvision.launcher.data.device.AppsProvider;
import com.hotelvision.launcher.data.device.InputsProvider;
import com.hotelvision.launcher.data.device.RecommendationsProvider;
import com.hotelvision.launcher.data.device.WhitelistAppsProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class LauncherRepository_Factory implements Factory<LauncherRepository> {
  private final Provider<LauncherApiService> apiProvider;

  private final Provider<LauncherDao> daoProvider;

  private final Provider<AppsProvider> appsProvider;

  private final Provider<WhitelistAppsProvider> whitelistAppsProvider;

  private final Provider<InputsProvider> inputsProvider;

  private final Provider<RecommendationsProvider> recommendationsProvider;

  private final Provider<LiveTvRepository> liveTvRepositoryProvider;

  public LauncherRepository_Factory(Provider<LauncherApiService> apiProvider,
      Provider<LauncherDao> daoProvider, Provider<AppsProvider> appsProvider,
      Provider<WhitelistAppsProvider> whitelistAppsProvider,
      Provider<InputsProvider> inputsProvider,
      Provider<RecommendationsProvider> recommendationsProvider,
      Provider<LiveTvRepository> liveTvRepositoryProvider) {
    this.apiProvider = apiProvider;
    this.daoProvider = daoProvider;
    this.appsProvider = appsProvider;
    this.whitelistAppsProvider = whitelistAppsProvider;
    this.inputsProvider = inputsProvider;
    this.recommendationsProvider = recommendationsProvider;
    this.liveTvRepositoryProvider = liveTvRepositoryProvider;
  }

  @Override
  public LauncherRepository get() {
    return newInstance(apiProvider.get(), daoProvider.get(), appsProvider.get(), whitelistAppsProvider.get(), inputsProvider.get(), recommendationsProvider.get(), liveTvRepositoryProvider.get());
  }

  public static LauncherRepository_Factory create(Provider<LauncherApiService> apiProvider,
      Provider<LauncherDao> daoProvider, Provider<AppsProvider> appsProvider,
      Provider<WhitelistAppsProvider> whitelistAppsProvider,
      Provider<InputsProvider> inputsProvider,
      Provider<RecommendationsProvider> recommendationsProvider,
      Provider<LiveTvRepository> liveTvRepositoryProvider) {
    return new LauncherRepository_Factory(apiProvider, daoProvider, appsProvider, whitelistAppsProvider, inputsProvider, recommendationsProvider, liveTvRepositoryProvider);
  }

  public static LauncherRepository newInstance(LauncherApiService api, LauncherDao dao,
      AppsProvider appsProvider, WhitelistAppsProvider whitelistAppsProvider,
      InputsProvider inputsProvider, RecommendationsProvider recommendationsProvider,
      LiveTvRepository liveTvRepository) {
    return new LauncherRepository(api, dao, appsProvider, whitelistAppsProvider, inputsProvider, recommendationsProvider, liveTvRepository);
  }
}
