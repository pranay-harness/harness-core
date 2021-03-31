package io.harness.gitsync;

import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;

import io.harness.SCMJavaClientModule;
import io.harness.gitsync.events.GitSyncConfigEventMessageListener;
import io.harness.gitsync.gittoharness.ChangeSetInterceptorService;
import io.harness.gitsync.gittoharness.GitToHarnessProcessor;
import io.harness.gitsync.gittoharness.GitToHarnessProcessorImpl;
import io.harness.gitsync.gittoharness.NoOpChangeSetInterceptorServiceImpl;
import io.harness.gitsync.persistance.EntityKeySource;
import io.harness.gitsync.persistance.EntityLookupHelper;
import io.harness.gitsync.sdk.GitSyncGrpcClientModule;
import io.harness.gitsync.sdk.GitSyncSdkGrpcServerModule;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

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
    //    bind(new TypeLiteral<GitAwareRepository<?, ?, ?>>() {}).to(new TypeLiteral<GitAwareRepositoryImpl<?, ?, ?>>()
    //    {});
    bind(GitToHarnessProcessor.class).to(GitToHarnessProcessorImpl.class);
    bind(ChangeSetInterceptorService.class).to(NoOpChangeSetInterceptorServiceImpl.class);
    bind(EntityKeySource.class).to(EntityLookupHelper.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(GIT_CONFIG_STREAM))
        .to(GitSyncConfigEventMessageListener.class);
  }
}
