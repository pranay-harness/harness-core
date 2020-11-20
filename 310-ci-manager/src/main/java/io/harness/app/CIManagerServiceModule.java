package io.harness.app;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.CIExecutionServiceModule;
import io.harness.app.impl.CIBuildInfoServiceImpl;
import io.harness.app.impl.YAMLToObjectImpl;
import io.harness.app.intfc.CIBuildInfoService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.connector.apis.client.ConnectorResourceClientModule;
import io.harness.core.ci.services.BuildNumberService;
import io.harness.core.ci.services.BuildNumberServiceImpl;
import io.harness.delegate.task.HDelegateTask;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.logserviceclient.CILogServiceClientModule;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.MongoPersistence;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.ngpipeline.pipeline.service.NGPipelineServiceImpl;
import io.harness.persistence.HPersistence;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.secrets.SecretNGManagerClientModule;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.states.CIDelegateTaskExecutor;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class CIManagerServiceModule extends AbstractModule {
  private final CIManagerConfiguration ciManagerConfiguration;

  public CIManagerServiceModule(CIManagerConfiguration ciManagerConfiguration) {
    this.ciManagerConfiguration = ciManagerConfiguration;
  }

  @Provides
  @Singleton
  @Named("serviceSecret")
  String serviceSecret() {
    return ciManagerConfiguration.getManagerServiceSecret();
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return (Supplier<DelegateCallbackToken>) Suppliers.memoize(
        () -> getDelegateCallbackToken(delegateServiceGrpcClient, ciManagerConfiguration));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, CIManagerConfiguration appConfig) {
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("ciManager")
                                  .setConnection(appConfig.getHarnessCIMongo().getUri())
                                  .build())
            .build());
    log.info("Delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  protected void configure() {
    bind(CIManagerConfiguration.class).toInstance(ciManagerConfiguration);
    bind(YAMLToObject.class).toInstance(new YAMLToObjectImpl());
    bind(HPersistence.class).to(MongoPersistence.class).in(Singleton.class);
    bind(NGPipelineService.class).to(NGPipelineServiceImpl.class);
    bind(CIBuildInfoService.class).to(CIBuildInfoServiceImpl.class);
    bind(BuildNumberService.class).to(BuildNumberServiceImpl.class);

    // Keeping it to 1 thread to start with. Assuming executor service is used only to
    // serve health checks. If it's being used for other tasks also, max pool size should be increased.
    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 2, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-ci-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));

    MapBinder<String, TaskExecutor<HDelegateTask>> taskExecutorMap = MapBinder.newMapBinder(
        binder(), new TypeLiteral<String>() {}, new TypeLiteral<TaskExecutor<HDelegateTask>>() {});
    taskExecutorMap.addBinding(TaskMode.DELEGATE_TASK_V3.name()).to(CIDelegateTaskExecutor.class);

    install(new CIExecutionServiceModule(ciManagerConfiguration.getCiExecutionServiceConfig()));
    install(DelegateServiceDriverModule.getInstance());
    install(new DelegateServiceDriverGrpcClientModule(ciManagerConfiguration.getManagerServiceSecret(),
        ciManagerConfiguration.getManagerTarget(), ciManagerConfiguration.getManagerAuthority(), null));
    install(new ManagerGrpcClientModule(ManagerGrpcClientModule.Config.builder()
                                            .target(ciManagerConfiguration.getManagerTarget())
                                            .authority(ciManagerConfiguration.getManagerAuthority())
                                            .build()));

    install(new SecretManagementClientModule(ciManagerConfiguration.getManagerClientConfig(),
        ciManagerConfiguration.getNgManagerServiceSecret(), "NextGenManager"));
    install(new EntitySetupUsageClientModule(ciManagerConfiguration.getNgManagerClientConfig(),
        ciManagerConfiguration.getNgManagerServiceSecret(), "CIManager"));
    install(new ConnectorResourceClientModule(ciManagerConfiguration.getNgManagerClientConfig(),
        ciManagerConfiguration.getNgManagerServiceSecret(), "CIManager"));
    install(new SecretNGManagerClientModule(ciManagerConfiguration.getNgManagerClientConfig(),
        ciManagerConfiguration.getNgManagerServiceSecret(), "CIManager"));
    install(new CILogServiceClientModule(ciManagerConfiguration.getLogServiceConfig()));
  }
}
