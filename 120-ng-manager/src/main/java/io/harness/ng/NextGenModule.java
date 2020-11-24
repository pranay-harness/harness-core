package io.harness.ng;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;

import io.harness.NGTriggersModule;
import io.harness.OrchestrationModule;
import io.harness.OrchestrationModuleConfig;
import io.harness.OrchestrationStepsModule;
import io.harness.OrchestrationVisualizationModule;
import io.harness.YamlBaseUrlServiceImpl;
import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.cdng.NGModule;
import io.harness.cdng.expressions.CDExpressionEvaluatorProvider;
import io.harness.cdng.orchestration.NgStepRegistrar;
import io.harness.connector.ConnectorModule;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.eventsframework.EventDrivenClient;
import io.harness.eventsframework.NoOpEventClient;
import io.harness.eventsframework.RedisStreamClient;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.gitsync.GitSyncModule;
import io.harness.gitsync.core.impl.GitSyncManagerInterfaceImpl;
import io.harness.govern.ProviderModule;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.DefaultOrganizationModule;
import io.harness.ng.core.InviteModule;
import io.harness.ng.core.SecretManagementModule;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.api.impl.NGSecretServiceV2Impl;
import io.harness.ng.core.api.impl.UserGroupServiceImpl;
import io.harness.ng.core.gitsync.GitChangeProcessorService;
import io.harness.ng.core.gitsync.GitSyncManagerInterface;
import io.harness.ng.core.gitsync.YamlHandler;
import io.harness.ng.core.impl.OrganizationServiceImpl;
import io.harness.ng.core.impl.ProjectServiceImpl;
import io.harness.ng.core.schema.YamlBaseUrlService;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.gitsync.NgCoreGitChangeSetProcessorServiceImpl;
import io.harness.ng.gitsync.handlers.ConnectorYamlHandler;
import io.harness.ng.orchestration.NgDelegate2TaskExecutor;
import io.harness.ngtriggers.service.TriggerWebhookService;
import io.harness.ngtriggers.service.impl.TriggerWebhookServiceImpl;
import io.harness.queue.QueueController;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.redesign.services.CustomExecutionServiceImpl;
import io.harness.redis.RedisConfig;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.NextGenRegistrars;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.spring.AliasRegistrar;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.version.VersionModule;
import io.harness.waiter.NgOrchestrationNotifyEventListener;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
public class NextGenModule extends AbstractModule {
  public static final String SECRET_MANAGER_CONNECTOR_SERVICE = "secretManagerConnectorService";
  public static final String CONNECTOR_DECORATOR_SERVICE = "connectorDecoratorService";
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
        .put(DelegateTaskProgressResponse.class, "ngManager_delegateTaskProgressResponses")
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
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("ngManager")
                                  .setConnection(appConfig.getMongoConfig().getUri())
                                  .build())
            .build());
    log.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());
    install(DelegateServiceDriverModule.getInstance());
    install(NGTriggersModule.getInstance());
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
    taskExecutorMap.addBinding(TaskMode.DELEGATE_TASK_V3.name()).to(NgDelegate2TaskExecutor.class);
    install(new ValidationModule(getValidatorFactory()));
    install(MongoModule.getInstance());
    install(new SpringPersistenceModule());
    install(new CoreModule());
    install(new InviteModule(this.appConfig.getServiceHttpClientConfig(),
        this.appConfig.getNextGenConfig().getManagerServiceSecret(), NG_MANAGER.getServiceId()));
    install(new ConnectorModule());
    install(new GitSyncModule());
    install(new DefaultOrganizationModule());
    install(NGModule.getInstance());
    install(new SecretManagementModule());
    install(new SecretManagementClientModule(this.appConfig.getServiceHttpClientConfig(),
        this.appConfig.getNextGenConfig().getNgManagerServiceSecret(), NG_MANAGER.getServiceId()));
    install(new DelegateServiceDriverGrpcClientModule(this.appConfig.getNextGenConfig().getManagerServiceSecret(),
        this.appConfig.getGrpcClientConfig().getTarget(), this.appConfig.getGrpcClientConfig().getAuthority(), null));
    install(new EntitySetupUsageClientModule(this.appConfig.getNgManagerClientConfig(),
        this.appConfig.getNextGenConfig().getNgManagerServiceSecret(), NG_MANAGER.getServiceId()));
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

      @Provides
      @Singleton
      Set<Class<? extends AliasRegistrar>> aliasRegistrars() {
        return ImmutableSet.<Class<? extends AliasRegistrar>>builder()
            .addAll(NextGenRegistrars.aliasRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(ManagerRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(ManagerRegistrars.springConverters)
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

    install(OrchestrationModule.getInstance());
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    install(ExecutionPlanModule.getInstance());

    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(NgStepRegistrar.class.getName()).to(NgStepRegistrar.class);
    bind(ProjectService.class).to(ProjectServiceImpl.class);
    bind(OrganizationService.class).to(OrganizationServiceImpl.class);
    bind(NGSecretServiceV2.class).to(NGSecretServiceV2Impl.class);
    bind(GitSyncManagerInterface.class).to(GitSyncManagerInterfaceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));
    bind(ConnectorService.class).annotatedWith(Names.named(CONNECTOR_DECORATOR_SERVICE)).to(ConnectorServiceImpl.class);
    bind(ConnectorService.class)
        .annotatedWith(Names.named(SECRET_MANAGER_CONNECTOR_SERVICE))
        .to(SecretManagerConnectorServiceImpl.class);
    bind(UserGroupService.class).to(UserGroupServiceImpl.class);
    bind(GitChangeProcessorService.class).to(NgCoreGitChangeSetProcessorServiceImpl.class);
    bindYamlHandlers();
    bind(YamlBaseUrlService.class).to(YamlBaseUrlServiceImpl.class);
    bind(TriggerWebhookService.class).to(TriggerWebhookServiceImpl.class);
  }

  @Provides
  @Singleton
  public EventDrivenClient getEventDrivenClient() {
    RedisConfig config = this.appConfig.getEventsFrameworkConfiguration().getRedisConfig();
    if (config.getRedisUrl().equals("dummyRedisUrl")) {
      return new NoOpEventClient();
    } else {
      return new RedisStreamClient(config);
    }
  }

  @Provides
  @Singleton
  public OrchestrationModuleConfig orchestrationModuleConfig() {
    return OrchestrationModuleConfig.builder()
        .expressionEvaluatorProvider(new CDExpressionEvaluatorProvider())
        .publisherName(NgOrchestrationNotifyEventListener.NG_ORCHESTRATION)
        .build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  private void bindYamlHandlers() {
    final Multibinder<YamlHandler> yamlHandlerMultiBinder = Multibinder.newSetBinder(binder(), YamlHandler.class);
    yamlHandlerMultiBinder.addBinding().to(ConnectorYamlHandler.class);
  }
}
