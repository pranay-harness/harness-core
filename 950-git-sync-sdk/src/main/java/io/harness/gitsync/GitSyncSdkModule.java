package io.harness.gitsync;

import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceImpl;
import io.harness.gitsync.persistance.GitAwareRepository;
import io.harness.gitsync.persistance.GitAwareRepositoryImpl;
import io.harness.gitsync.sdk.GitSyncGrpcClientModule;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class GitSyncSdkModule extends AbstractModule {
  private static volatile GitSyncSdkModule instance;

  static GitSyncSdkModule getInstance() {
    if (instance == null) {
      instance = new GitSyncSdkModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(GitAwarePersistence.class).to(GitAwarePersistenceImpl.class);
    bind(GitAwareRepository.class).to(GitAwareRepositoryImpl.class);
    bind(new TypeLiteral<GitAwareRepository<?, ?, ?>>() {}).to(new TypeLiteral<GitAwareRepositoryImpl<?, ?, ?>>() {});
    install(GitSyncGrpcClientModule.getInstance());
  }
}
