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
public final class WhitelistAppsProviderImpl_Factory implements Factory<WhitelistAppsProviderImpl> {
  private final Provider<Context> contextProvider;

  public WhitelistAppsProviderImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WhitelistAppsProviderImpl get() {
    return newInstance(contextProvider.get());
  }

  public static WhitelistAppsProviderImpl_Factory create(Provider<Context> contextProvider) {
    return new WhitelistAppsProviderImpl_Factory(contextProvider);
  }

  public static WhitelistAppsProviderImpl newInstance(Context context) {
    return new WhitelistAppsProviderImpl(context);
  }
}
