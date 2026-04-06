package com.hotelvision.launcher.di;

import com.hotelvision.launcher.data.api.LauncherApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
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
public final class NetworkModule_ProvideLauncherApiServiceFactory implements Factory<LauncherApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public NetworkModule_ProvideLauncherApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public LauncherApiService get() {
    return provideLauncherApiService(retrofitProvider.get());
  }

  public static NetworkModule_ProvideLauncherApiServiceFactory create(
      Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideLauncherApiServiceFactory(retrofitProvider);
  }

  public static LauncherApiService provideLauncherApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideLauncherApiService(retrofit));
  }
}
