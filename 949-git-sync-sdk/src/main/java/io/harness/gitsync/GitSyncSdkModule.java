package io.harness.gitsync;

import io.harness.SCMJavaClientModule;
import io.harness.gitsync.gittoharness.ChangeSetInterceptorService;
import io.harness.gitsync.gittoharness.GitToHarnessProcessor;
import io.harness.gitsync.gittoharness.GitToHarnessProcessorImpl;
import io.harness.gitsync.gittoharness.NoOpChangeSetInterceptorServiceImpl;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceImpl;
import io.harness.gitsync.persistance.GitAwareRepository;
import io.harness.gitsync.persistance.GitAwareRepositoryImpl;
import io.harness.gitsync.sdk.GitSyncGrpcClientModule;
import io.harness.gitsync.sdk.GitSyncSdkGrpcServerModule;

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
    install(GitSyncGrpcClientModule.getInstance());
    install(GitSyncSdkGrpcServerModule.getInstance());
    install(SCMJavaClientModule.getInstance());
    bind(GitAwarePersistence.class).to(GitAwarePersistenceImpl.class);
    bind(new TypeLiteral<GitAwareRepository<?, ?, ?>>() {}).to(new TypeLiteral<GitAwareRepositoryImpl<?, ?, ?>>() {});
    bind(GitAwarePersistence.class).to(GitAwarePersistenceImpl.class);
    bind(GitToHarnessProcessor.class).to(GitToHarnessProcessorImpl.class);
    bind(ChangeSetInterceptorService.class).to(NoOpChangeSetInterceptorServiceImpl.class);
  }
}
