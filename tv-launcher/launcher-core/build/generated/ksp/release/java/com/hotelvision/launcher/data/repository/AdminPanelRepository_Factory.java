package com.hotelvision.launcher.data.repository;

import android.content.Context;
import com.hotelvision.launcher.data.api.LauncherApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AdminPanelRepository_Factory implements Factory<AdminPanelRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<LauncherApiService> apiProvider;

  private final Provider<Json> jsonProvider;

  public AdminPanelRepository_Factory(Provider<Context> contextProvider,
      Provider<LauncherApiService> apiProvider, Provider<Json> jsonProvider) {
    this.contextProvider = contextProvider;
    this.apiProvider = apiProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public AdminPanelRepository get() {
    return newInstance(contextProvider.get(), apiProvider.get(), jsonProvider.get());
  }

  public static AdminPanelRepository_Factory create(Provider<Context> contextProvider,
      Provider<LauncherApiService> apiProvider, Provider<Json> jsonProvider) {
    return new AdminPanelRepository_Factory(contextProvider, apiProvider, jsonProvider);
  }

  public static AdminPanelRepository newInstance(Context context, LauncherApiService api,
      Json json) {
    return new AdminPanelRepository(context, api, json);
  }
}
