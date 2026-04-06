package com.hotelvision.launcher.di;

import android.content.Context;
import com.hotelvision.launcher.data.db.LauncherDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideLauncherDatabaseFactory implements Factory<LauncherDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvideLauncherDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LauncherDatabase get() {
    return provideLauncherDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideLauncherDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideLauncherDatabaseFactory(contextProvider);
  }

  public static LauncherDatabase provideLauncherDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLauncherDatabase(context));
  }
}
