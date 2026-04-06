package com.hotelvision.launcher.di;

import android.content.Context;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.WorkManager;
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
public final class WorkerModule_ProvideWorkManagerFactory implements Factory<WorkManager> {
  private final Provider<Context> contextProvider;

  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public WorkerModule_ProvideWorkManagerFactory(Provider<Context> contextProvider,
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.contextProvider = contextProvider;
    this.workerFactoryProvider = workerFactoryProvider;
  }

  @Override
  public WorkManager get() {
    return provideWorkManager(contextProvider.get(), workerFactoryProvider.get());
  }

  public static WorkerModule_ProvideWorkManagerFactory create(Provider<Context> contextProvider,
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new WorkerModule_ProvideWorkManagerFactory(contextProvider, workerFactoryProvider);
  }

  public static WorkManager provideWorkManager(Context context, HiltWorkerFactory workerFactory) {
    return Preconditions.checkNotNullFromProvides(WorkerModule.INSTANCE.provideWorkManager(context, workerFactory));
  }
}
