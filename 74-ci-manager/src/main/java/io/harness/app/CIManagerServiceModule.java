package io.harness.app;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import io.harness.CIExecutionServiceModule;
import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.app.impl.CIPipelineServiceImpl;
import io.harness.app.impl.YAMLToObjectImpl;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.govern.DependencyModule;
import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.persistence.HPersistence;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
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

  @Provides
  @Singleton
  ManagerClientFactory managerClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new ManagerClientFactory(managerBaseUrl, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Provides
  @Singleton
  @Named("serviceSecret")
  String serviceSecret() {
    return ciManagerConfiguration.getDelegateGrpcServiceTokenSecret();
  }

  @Override
  protected void configure() {
    MapBinder<String, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), String.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskMode.DELEGATE_TASK_V1.name()).to(EmptyTaskExecutor.class);
    bind(CIManagerConfiguration.class).toInstance(ciManagerConfiguration);
    bind(YAMLToObject.class).toInstance(new YAMLToObjectImpl());
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(CIPipelineService.class).to(CIPipelineServiceImpl.class);
    bind(ManagerCIResource.class).toProvider(ManagerClientFactory.class);
    bind(CIServiceAuthSecretKey.class).to(CIServiceAuthSecretKeyImpl.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(
        OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                            .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                            .build()),
        CIExecutionServiceModule.getInstance());
  }
}
