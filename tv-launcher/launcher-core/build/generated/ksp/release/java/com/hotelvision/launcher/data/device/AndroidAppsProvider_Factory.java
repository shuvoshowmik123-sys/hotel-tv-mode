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
public final class AndroidAppsProvider_Factory implements Factory<AndroidAppsProvider> {
  private final Provider<Context> contextProvider;

  public AndroidAppsProvider_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AndroidAppsProvider get() {
    return newInstance(contextProvider.get());
  }

  public static AndroidAppsProvider_Factory create(Provider<Context> contextProvider) {
    return new AndroidAppsProvider_Factory(contextProvider);
  }

  public static AndroidAppsProvider newInstance(Context context) {
    return new AndroidAppsProvider(context);
  }
}
