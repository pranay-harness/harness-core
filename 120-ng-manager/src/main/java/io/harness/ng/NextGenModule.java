package io.harness.ng;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.harness.ManagerDelegateServiceDriverModule;
import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.cdng.NGModule;
import io.harness.cdng.expressions.CDExpressionEvaluatorProvider;
import io.harness.cdng.orchestration.NgStepRegistrar;
import io.harness.connector.ConnectorModule;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.gitsync.GitSyncModule;
import io.harness.gitsync.core.impl.GitSyncManagerInterfaceImpl;
import io.harness.govern.DependencyModule;
import io.harness.govern.ProviderModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.grpc.DelegateServiceGrpcClientModule;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.NgAsyncTaskGrpcServerModule;
import io.harness.ng.core.SecretManagementModule;
import io.harness.ng.core.gitsync.GitSyncManagerInterface;
import io.harness.ng.core.remote.server.grpc.perpetualtask.RemotePerpetualTaskServiceClientManager;
import io.harness.ng.core.remote.server.grpc.perpetualtask.impl.RemotePerpetualTaskServiceClientManagerImpl;
import io.harness.ng.orchestration.NgDelegateTaskExecutor;
import io.harness.queue.QueueController;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.redesign.services.CustomExecutionServiceImpl;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.version.VersionModule;
import io.harness.waiter.NgOrchestrationNotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class NextGenModule extends DependencyModule {
  private final NextGenConfiguration appConfig;

  public NextGenModule(NextGenConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder()
        .put(DelegateSyncTaskResponse.class, "ngManager_delegateSyncTaskResponses")
        .put(DelegateAsyncTaskResponse.class, "ngManager_delegateAsyncTaskResponses")
        .build();
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return Suppliers.memoize(() -> getDelegateCallbackToken(delegateServiceGrpcClient, appConfig));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, NextGenConfiguration appConfig) {
    logger.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("ngManager")
                                  .setConnection(appConfig.getMongoConfig().getUri())
                                  .build())
            .build());
    logger.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());

    bind(NextGenConfiguration.class).toInstance(appConfig);

    install(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getMongoConfig();
      }
    });

    /*
    [secondary-db]: To use another DB, uncomment this and add @Named("primaryMongoConfig") to the above one

    install(new ProviderModule() {
       @Provides
       @Singleton
       @Named("secondaryMongoConfig")
       MongoConfig mongoConfig() {
         return appConfig.getSecondaryMongoConfig();
       }
     });*/
    bind(CustomExecutionService.class).to(CustomExecutionServiceImpl.class);
    MapBinder<String, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), String.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskMode.DELEGATE_TASK_V2.name()).to(NgDelegateTaskExecutor.class);
    install(new ValidationModule(getValidatorFactory()));
    install(new NextGenPersistenceModule());
    install(new CoreModule());
    install(new ConnectorModule());
    install(new GitSyncModule());
    install(NGModule.getInstance());
    install(new SecretManagementModule());
    install(new SecretManagementClientModule(
        this.appConfig.getSecretManagerClientConfig(), this.appConfig.getNextGenConfig().getManagerServiceSecret()));
    install(new ManagerDelegateServiceDriverModule(this.appConfig.getGrpcClientConfig(),
        this.appConfig.getNextGenConfig().getManagerServiceSecret(), NextGenConfiguration.SERVICE_ID));
    install(new NgAsyncTaskGrpcServerModule(
        this.appConfig.getGrpcServerConfig(), "manager", this.appConfig.getNextGenConfig().getManagerServiceSecret()));
    install(new DelegateServiceGrpcClientModule(this.appConfig.getNextGenConfig().getManagerServiceSecret()));
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(NextGenRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(NextGenRegistrars.morphiaRegistrars)
            .build();
      }
    });
    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(NgStepRegistrar.class.getName()).to(NgStepRegistrar.class);

    bind(RemotePerpetualTaskServiceClientManager.class).to(RemotePerpetualTaskServiceClientManagerImpl.class);

    bind(GitSyncManagerInterface.class).to(GitSyncManagerInterfaceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(
        OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                            .expressionEvaluatorProvider(new CDExpressionEvaluatorProvider())
                                            .publisherName(NgOrchestrationNotifyEventListener.NG_ORCHESTRATION)
                                            .build()),
        ExecutionPlanModule.getInstance(), DelegateServiceDriverModule.getInstance());
  }
}
