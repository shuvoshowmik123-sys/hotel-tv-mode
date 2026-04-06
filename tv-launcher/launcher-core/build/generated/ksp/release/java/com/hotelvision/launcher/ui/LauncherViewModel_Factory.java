package com.hotelvision.launcher.ui;

import com.hotelvision.launcher.data.db.dao.LauncherDao;
import com.hotelvision.launcher.data.repository.AdminPanelRepository;
import com.hotelvision.launcher.data.repository.LauncherRepository;
import com.hotelvision.launcher.data.session.GuestPersonalizationManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LauncherViewModel_Factory implements Factory<LauncherViewModel> {
  private final Provider<LauncherRepository> repositoryProvider;

  private final Provider<LauncherDao> daoProvider;

  private final Provider<GuestPersonalizationManager> guestPersonalizationManagerProvider;

  private final Provider<AdminPanelRepository> adminPanelRepositoryProvider;

  public LauncherViewModel_Factory(Provider<LauncherRepository> repositoryProvider,
      Provider<LauncherDao> daoProvider,
      Provider<GuestPersonalizationManager> guestPersonalizationManagerProvider,
      Provider<AdminPanelRepository> adminPanelRepositoryProvider) {
    this.repositoryProvider = repositoryProvider;
    this.daoProvider = daoProvider;
    this.guestPersonalizationManagerProvider = guestPersonalizationManagerProvider;
    this.adminPanelRepositoryProvider = adminPanelRepositoryProvider;
  }

  @Override
  public LauncherViewModel get() {
    return newInstance(repositoryProvider.get(), daoProvider.get(), guestPersonalizationManagerProvider.get(), adminPanelRepositoryProvider.get());
  }

  public static LauncherViewModel_Factory create(Provider<LauncherRepository> repositoryProvider,
      Provider<LauncherDao> daoProvider,
      Provider<GuestPersonalizationManager> guestPersonalizationManagerProvider,
      Provider<AdminPanelRepository> adminPanelRepositoryProvider) {
    return new LauncherViewModel_Factory(repositoryProvider, daoProvider, guestPersonalizationManagerProvider, adminPanelRepositoryProvider);
  }

  public static LauncherViewModel newInstance(LauncherRepository repository, LauncherDao dao,
      GuestPersonalizationManager guestPersonalizationManager,
      AdminPanelRepository adminPanelRepository) {
    return new LauncherViewModel(repository, dao, guestPersonalizationManager, adminPanelRepository);
  }
}
