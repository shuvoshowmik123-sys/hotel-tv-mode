package com.hotelvision.launcher.data.device;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AndroidTvRecommendationsProvider_Factory implements Factory<AndroidTvRecommendationsProvider> {
  private final Provider<Context> contextProvider;

  public AndroidTvRecommendationsProvider_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AndroidTvRecommendationsProvider get() {
    return newInstance(contextProvider.get());
  }

  public static AndroidTvRecommendationsProvider_Factory create(Provider<Context> contextProvider) {
    return new AndroidTvRecommendationsProvider_Factory(contextProvider);
  }

  public static AndroidTvRecommendationsProvider newInstance(Context context) {
    return new AndroidTvRecommendationsProvider(context);
  }
}
