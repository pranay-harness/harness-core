package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;

import io.harness.SCMJavaClientModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.events.GitSyncConfigEventMessageListener;
import io.harness.gitsync.gittoharness.ChangeSetHelperServiceImpl;
import io.harness.gitsync.gittoharness.ChangeSetInterceptorService;
import io.harness.gitsync.gittoharness.GitSdkInterface;
import io.harness.gitsync.gittoharness.GitToHarnessProcessor;
import io.harness.gitsync.gittoharness.GitToHarnessProcessorImpl;
import io.harness.gitsync.gittoharness.NoOpChangeSetInterceptorServiceImpl;
import io.harness.gitsync.persistance.EntityKeySource;
import io.harness.gitsync.persistance.EntityLookupHelper;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitAwarePersistenceNewImpl;
import io.harness.gitsync.scm.ScmDelegateGitHelper;
import io.harness.gitsync.scm.ScmGitHelper;
import io.harness.gitsync.scm.ScmManagerGitHelper;
import io.harness.gitsync.sdk.GitSyncGrpcClientModule;
import io.harness.gitsync.sdk.GitSyncSdkGrpcServerModule;
import io.harness.ng.core.event.MessageListener;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(DX)
public class GitSyncSdkModule extends AbstractModule {
  public static final String SCM_ON_DELEGATE = "scmOnDelegate";
  public static final String SCM_ON_MANAGER = "scmOnManager";

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
    bind(GitSdkInterface.class).to(ChangeSetHelperServiceImpl.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(GIT_CONFIG_STREAM))
        .to(GitSyncConfigEventMessageListener.class);
    bind(GitAwarePersistence.class).to(GitAwarePersistenceNewImpl.class);
    bind(ScmGitHelper.class).annotatedWith(Names.named(SCM_ON_MANAGER)).to(ScmManagerGitHelper.class);
    bind(ScmGitHelper.class).annotatedWith(Names.named(SCM_ON_DELEGATE)).to(ScmDelegateGitHelper.class);
    //    AnnotationConfigApplicationContext context =
    //            new AnnotationConfigApplicationContext(GitAwarePersistenceBean.class);
    //    Injector injector = new SpringInjector(context);

    //    install(new SpringModule(BeanFactoryProvider.from(GitAwarePersistenceBean.class)));
  }
}
