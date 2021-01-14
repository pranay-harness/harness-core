package io.harness.ng;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ORGANIZATION_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.PROJECT_ENTITY;

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
import io.harness.connector.ConnectorModule;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.gitsync.GitSyncModule;
import io.harness.gitsync.core.impl.GitSyncManagerInterfaceImpl;
import io.harness.govern.ProviderModule;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.modules.ModulesClientModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.DefaultOrganizationModule;
import io.harness.ng.core.InviteModule;
import io.harness.ng.core.NGAggregateModule;
import io.harness.ng.core.SecretManagementModule;
import io.harness.ng.core.api.NGModulesService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.api.impl.NGModulesServiceImpl;
import io.harness.ng.core.api.impl.NGSecretServiceV2Impl;
import io.harness.ng.core.api.impl.UserGroupServiceImpl;
import io.harness.ng.core.entityactivity.event.EntityActivityCrudEventMessageProcessor;
import io.harness.ng.core.entitysetupusage.event.SetupUsageChangeEventMessageProcessor;
import io.harness.ng.core.event.ConnectorEntityCRUDStreamListener;
import io.harness.ng.core.event.ConnectorFeatureFlagStreamListener;
import io.harness.ng.core.event.MessageListener;
import io.harness.ng.core.event.MessageProcessor;
import io.harness.ng.core.event.OrganizationEntityCRUDStreamListener;
import io.harness.ng.core.event.OrganizationFeatureFlagStreamListener;
import io.harness.ng.core.event.ProjectEntityCRUDStreamListener;
import io.harness.ng.core.gitsync.GitChangeProcessorService;
import io.harness.ng.core.gitsync.GitSyncManagerInterface;
import io.harness.ng.core.gitsync.YamlHandler;
import io.harness.ng.core.impl.OrganizationServiceImpl;
import io.harness.ng.core.impl.ProjectServiceImpl;
import io.harness.ng.core.schema.YamlBaseUrlService;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.eventsframework.EventsFrameworkModule;
import io.harness.ng.gitsync.NgCoreGitChangeSetProcessorServiceImpl;
import io.harness.ng.gitsync.handlers.ConnectorYamlHandler;
import io.harness.queue.QueueController;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.redesign.services.CustomExecutionServiceImpl;
import io.harness.redis.RedisConfig;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.NextGenRegistrars;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.version.VersionModule;
import io.harness.waiter.NgOrchestrationNotifyEventListener;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
    bind(RedisConfig.class)
        .annotatedWith(Names.named("lock"))
        .toInstance(appConfig.getEventsFrameworkConfiguration().getRedisConfig());
    install(new ValidationModule(getValidatorFactory()));
    install(MongoModule.getInstance());
    install(new NextGenPersistenceModule(appConfig.getShouldConfigureWithPMS()));
    install(new CoreModule());
    install(new InviteModule(this.appConfig.getServiceHttpClientConfig(),
        this.appConfig.getNextGenConfig().getManagerServiceSecret(), NG_MANAGER.getServiceId()));
    install(new ConnectorModule(this.appConfig.getCeAwsSetupConfig()));
    install(new GitSyncModule());
    install(new DefaultOrganizationModule());
    install(new NGAggregateModule());
    install(NGModule.getInstance(getOrchestrationConfig()));
    install(new EventsFrameworkModule(this.appConfig.getEventsFrameworkConfiguration()));
    install(new SecretManagementModule());
    install(new SecretManagementClientModule(this.appConfig.getServiceHttpClientConfig(),
        this.appConfig.getNextGenConfig().getNgManagerServiceSecret(), NG_MANAGER.getServiceId()));
    install(new DelegateServiceDriverGrpcClientModule(this.appConfig.getNextGenConfig().getManagerServiceSecret(),
        this.appConfig.getGrpcClientConfig().getTarget(), this.appConfig.getGrpcClientConfig().getAuthority()));
    install(new EntitySetupUsageClientModule(this.appConfig.getNgManagerClientConfig(),
        this.appConfig.getNextGenConfig().getNgManagerServiceSecret(), NG_MANAGER.getServiceId()));
    install(new ModulesClientModule(this.appConfig.getServiceHttpClientConfig(),
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

    install(OrchestrationModule.getInstance(getOrchestrationConfig()));
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    install(ExecutionPlanModule.getInstance());

    bind(ProjectService.class).to(ProjectServiceImpl.class);
    bind(OrganizationService.class).to(OrganizationServiceImpl.class);
    bind(NGModulesService.class).to(NGModulesServiceImpl.class);
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

    bind(MessageProcessor.class)
        .annotatedWith(Names.named(EventsFrameworkMetadataConstants.SETUP_USAGE_ENTITY))
        .to(SetupUsageChangeEventMessageProcessor.class);
    bind(MessageProcessor.class)
        .annotatedWith(Names.named(EventsFrameworkMetadataConstants.ACTIVITY_ENTITY))
        .to(EntityActivityCrudEventMessageProcessor.class);

    registerEventsFrameworkMessageListeners();
  }

  private void registerEventsFrameworkMessageListeners() {
    bind(MessageListener.class)
        .annotatedWith(Names.named(ORGANIZATION_ENTITY + ENTITY_CRUD))
        .to(OrganizationEntityCRUDStreamListener.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(PROJECT_ENTITY + ENTITY_CRUD))
        .to(ProjectEntityCRUDStreamListener.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(CONNECTOR_ENTITY + ENTITY_CRUD))
        .to(ConnectorEntityCRUDStreamListener.class);

    bind(MessageListener.class)
        .annotatedWith(Names.named(ORGANIZATION_ENTITY + FEATURE_FLAG_STREAM))
        .to(OrganizationFeatureFlagStreamListener.class);
    bind(MessageListener.class)
        .annotatedWith(Names.named(CONNECTOR_ENTITY + FEATURE_FLAG_STREAM))
        .to(ConnectorFeatureFlagStreamListener.class);
  }

  private OrchestrationModuleConfig getOrchestrationConfig() {
    return OrchestrationModuleConfig.builder()
        .serviceName("CD_NG")
        .withPMS(appConfig.getShouldConfigureWithPMS())
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
