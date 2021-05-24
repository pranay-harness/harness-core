package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.SCMJavaClientModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.client.GitSyncSdkGrpcClientModule;
import io.harness.gitsync.common.impl.GitBranchServiceImpl;
import io.harness.gitsync.common.impl.GitEntityServiceImpl;
import io.harness.gitsync.common.impl.GitSyncSettingsServiceImpl;
import io.harness.gitsync.common.impl.HarnessToGitHelperServiceImpl;
import io.harness.gitsync.common.impl.ScmDelegateFacilitatorServiceImpl;
import io.harness.gitsync.common.impl.ScmManagerFacilitatorServiceImpl;
import io.harness.gitsync.common.impl.ScmOrchestratorServiceImpl;
import io.harness.gitsync.common.impl.YamlGitConfigServiceImpl;
import io.harness.gitsync.common.impl.gittoharness.GitToHarnessProcessorServiceImpl;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.GitSyncSettingsService;
import io.harness.gitsync.common.service.HarnessToGitHelperService;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.impl.GitCommitServiceImpl;
import io.harness.gitsync.core.impl.GitSyncTriggerServiceImpl;
import io.harness.gitsync.core.impl.YamlChangeSetServiceImpl;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.GitSyncTriggerService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.gitfileactivity.impl.GitSyncServiceImpl;
import io.harness.gitsync.gitfileactivity.service.GitSyncService;
import io.harness.gitsync.gitsyncerror.impl.GitSyncErrorServiceImpl;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.persistence.HPersistence;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class GitSyncModule extends AbstractModule {
  private static final AtomicReference<GitSyncModule> instanceRef = new AtomicReference<>();
  public static final String SCM_ON_MANAGER = "scmOnManager";
  public static final String SCM_ON_DELEGATE = "scmOnDelegate";

  public static GitSyncModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new GitSyncModule());
    }
    return instanceRef.get();
  }

  @Provides
  @Singleton
  Map<EntityType, Microservice> getEntityTypeMicroserviceMap() {
    return ImmutableMap.<EntityType, Microservice>builder()
        .put(EntityType.CONNECTORS, Microservice.CORE)
        .put(EntityType.PIPELINES, Microservice.PMS)
        .put(EntityType.INPUT_SETS, Microservice.PMS)
        .build();
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    install(SCMJavaClientModule.getInstance());
    install(GitSyncSdkGrpcClientModule.getInstance());
    bind(YamlGitConfigService.class).to(YamlGitConfigServiceImpl.class);
    bind(YamlChangeSetService.class).to(YamlChangeSetServiceImpl.class);
    bind(GitCommitService.class).to(GitCommitServiceImpl.class);
    bind(GitSyncService.class).to(GitSyncServiceImpl.class);
    bind(GitSyncErrorService.class).to(GitSyncErrorServiceImpl.class);
    bind(GitBranchService.class).to(GitBranchServiceImpl.class);
    bind(GitEntityService.class).to(GitEntityServiceImpl.class);
    bind(GitSyncTriggerService.class).to(GitSyncTriggerServiceImpl.class);
    bind(HarnessToGitHelperService.class).to(HarnessToGitHelperServiceImpl.class);
    bind(GitToHarnessProcessorService.class).to(GitToHarnessProcessorServiceImpl.class);
    bind(GitSyncSettingsService.class).to(GitSyncSettingsServiceImpl.class);
    bind(ScmClientFacilitatorService.class)
        .annotatedWith(Names.named(SCM_ON_MANAGER))
        .to(ScmManagerFacilitatorServiceImpl.class);
    bind(ScmClientFacilitatorService.class)
        .annotatedWith(Names.named(SCM_ON_DELEGATE))
        .to(ScmDelegateFacilitatorServiceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("gitChangeSet"))
        .toInstance(new ManagedScheduledExecutorService("GitChangeSet"));
    bind(ScmOrchestratorService.class).to(ScmOrchestratorServiceImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
