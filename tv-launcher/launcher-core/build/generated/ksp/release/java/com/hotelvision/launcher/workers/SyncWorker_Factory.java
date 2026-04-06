package com.hotelvision.launcher.workers;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.hotelvision.launcher.data.repository.LauncherRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SyncWorker_Factory {
  private final Provider<LauncherRepository> repositoryProvider;

  public SyncWorker_Factory(Provider<LauncherRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public SyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, repositoryProvider.get());
  }

  public static SyncWorker_Factory create(Provider<LauncherRepository> repositoryProvider) {
    return new SyncWorker_Factory(repositoryProvider);
  }

  public static SyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      LauncherRepository repository) {
    return new SyncWorker(appContext, workerParams, repository);
  }
}
