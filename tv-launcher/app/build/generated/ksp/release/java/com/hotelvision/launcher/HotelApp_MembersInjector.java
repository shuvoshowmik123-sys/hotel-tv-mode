package com.hotelvision.launcher;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class HotelApp_MembersInjector implements MembersInjector<HotelApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public HotelApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<HotelApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HotelApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(HotelApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.hotelvision.launcher.HotelApp.workerFactory")
  public static void injectWorkerFactory(HotelApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
