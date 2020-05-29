package io.harness.app;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

import io.harness.CIExecutionServiceModule;
import io.harness.OrchestrationModule;
import io.harness.app.impl.CIPipelineServiceImpl;
import io.harness.app.impl.YAMLToObjectImpl;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.DelegateTask;
import io.harness.govern.DependencyModule;
import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.persistence.HPersistence;
import io.harness.security.ServiceTokenGenerator;
import io.harness.tasks.TaskExecutor;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ci.CIServiceAuthSecretKey;
import software.wings.service.impl.ci.CIServiceAuthSecretKeyImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

import java.util.Set;

public class CIManagerServiceModule extends DependencyModule {
  private String managerBaseUrl;
  private final CIManagerConfiguration ciManagerConfiguration;

  public CIManagerServiceModule(CIManagerConfiguration ciManagerConfiguration, String managerBaseUrl) {
    this.ciManagerConfiguration = ciManagerConfiguration;
    this.managerBaseUrl = managerBaseUrl;
  }

  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    MapBinder<String, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), String.class, TaskExecutor.class);
    taskExecutorMap.addBinding(DelegateTask.TASK_IDENTIFIER).to(EmptyTaskExecutor.class);
    bind(CIManagerConfiguration.class).toInstance(ciManagerConfiguration);
    bind(YAMLToObject.class).toInstance(new YAMLToObjectImpl());
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(CIPipelineService.class).to(CIPipelineServiceImpl.class);
    bind(ManagerCIResource.class).toProvider(new ManagerClientFactory(managerBaseUrl, tokenGenerator));
    bind(CIServiceAuthSecretKey.class).to(CIServiceAuthSecretKeyImpl.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(OrchestrationModule.getInstance(), CIExecutionServiceModule.getInstance());
  }
}
