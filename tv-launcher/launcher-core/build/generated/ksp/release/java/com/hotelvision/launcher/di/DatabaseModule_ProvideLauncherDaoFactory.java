package com.hotelvision.launcher.di;

import com.hotelvision.launcher.data.db.LauncherDatabase;
import com.hotelvision.launcher.data.db.dao.LauncherDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideLauncherDaoFactory implements Factory<LauncherDao> {
  private final Provider<LauncherDatabase> databaseProvider;

  public DatabaseModule_ProvideLauncherDaoFactory(Provider<LauncherDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LauncherDao get() {
    return provideLauncherDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideLauncherDaoFactory create(
      Provider<LauncherDatabase> databaseProvider) {
    return new DatabaseModule_ProvideLauncherDaoFactory(databaseProvider);
  }

  public static LauncherDao provideLauncherDao(LauncherDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideLauncherDao(database));
  }
}
