package com.hotelvision.launcher.data.session;

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
public final class GuestPersonalizationManager_Factory implements Factory<GuestPersonalizationManager> {
  private final Provider<Context> contextProvider;

  public GuestPersonalizationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public GuestPersonalizationManager get() {
    return newInstance(contextProvider.get());
  }

  public static GuestPersonalizationManager_Factory create(Provider<Context> contextProvider) {
    return new GuestPersonalizationManager_Factory(contextProvider);
  }

  public static GuestPersonalizationManager newInstance(Context context) {
    return new GuestPersonalizationManager(context);
  }
}
