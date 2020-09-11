package io.harness.app;

import com.google.common.base.Suppliers;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationStepsModule;
import io.harness.app.impl.CIBuildInfoServiceImpl;
import io.harness.app.impl.CIPipelineServiceImpl;
import io.harness.app.impl.YAMLToObjectImpl;
import io.harness.app.intfc.CIBuildInfoService;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.core.ci.services.BuildNumberServiceImpl;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.govern.DependencyModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.grpc.DelegateServiceGrpcClientModule;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.persistence.HPersistence;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.waiter.OrchestrationNotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ci.CIServiceAuthSecretKey;
import software.wings.service.impl.ci.CIServiceAuthSecretKeyImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

@Slf4j
public class CIManagerServiceModule extends DependencyModule {
  private final String managerBaseUrl;
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

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return Suppliers.memoize(() -> getDelegateCallbackToken(delegateServiceGrpcClient, ciManagerConfiguration));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, CIManagerConfiguration appConfig) {
    logger.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("ciManager")
                                  .setConnection(appConfig.getHarnessCIMongo().getUri())
                                  .build())
            .build());
    logger.info("Delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  protected void configure() {
    bind(CIManagerConfiguration.class).toInstance(ciManagerConfiguration);
    bind(YAMLToObject.class).toInstance(new YAMLToObjectImpl());
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(CIPipelineService.class).to(CIPipelineServiceImpl.class);
    bind(CIBuildInfoService.class).to(CIBuildInfoServiceImpl.class);
    bind(ManagerCIResource.class).toProvider(ManagerClientFactory.class);
    bind(CIServiceAuthSecretKey.class).to(CIServiceAuthSecretKeyImpl.class);
    bind(BuildNumberService.class).to(BuildNumberServiceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));

    install(OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                                .publisherName(OrchestrationNotifyEventListener.ORCHESTRATION)
                                                .build()));
    install(OrchestrationStepsModule.getInstance());
    install(DelegateServiceDriverModule.getInstance());
    install(new DelegateServiceGrpcClientModule(ciManagerConfiguration.getManagerServiceSecret()));
    install(new ManagerGrpcClientModule(ManagerGrpcClientModule.Config.builder()
                                            .target(ciManagerConfiguration.getManagerTarget())
                                            .authority(ciManagerConfiguration.getManagerAuthority())
                                            .build()));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }
}
