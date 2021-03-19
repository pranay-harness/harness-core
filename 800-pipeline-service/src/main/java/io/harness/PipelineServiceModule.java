package io.harness;

import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.delegatelog.client.DelegateSelectionLogHttpClientModule;
import io.harness.engine.StepTypeLookupService;
import io.harness.entitysetupusageclient.EntitySetupUsageClientModule;
import io.harness.filter.FilterType;
import io.harness.filter.FiltersModule;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.grpc.server.PipelineServiceGrpcModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLockModule;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.UserClientModule;
import io.harness.ng.core.account.remote.AccountClientModule;
import io.harness.organizationmanagerclient.OrganizationManagementClientModule;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.pms.approval.ApprovalResourceService;
import io.harness.pms.approval.ApprovalResourceServiceImpl;
import io.harness.pms.barriers.service.PMSBarrierService;
import io.harness.pms.barriers.service.PMSBarrierServiceImpl;
import io.harness.pms.expressions.PMSExpressionEvaluatorProvider;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetServiceImpl;
import io.harness.pms.pipeline.mappers.PipelineFilterPropertiesMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineServiceImpl;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.pipeline.service.PMSYamlSchemaServiceImpl;
import io.harness.pms.plan.creation.NodeTypeLookupService;
import io.harness.pms.plan.creation.NodeTypeLookupServiceImpl;
import io.harness.pms.plan.execution.mapper.PipelineExecutionFilterPropertiesMapper;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.plan.execution.service.PMSExecutionServiceImpl;
import io.harness.pms.sdk.StepTypeLookupServiceImpl;
import io.harness.pms.triggers.webhook.service.TriggerWebhookExecutionService;
import io.harness.pms.triggers.webhook.service.impl.TriggerWebhookExecutionServiceImpl;
import io.harness.projectmanagerclient.ProjectManagementClientModule;
import io.harness.queue.QueueController;
import io.harness.redis.RedisConfig;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.OrchestrationStepsModuleRegistrars;
import io.harness.serializer.PipelineServiceModuleRegistrars;
import io.harness.service.PmsDelegateServiceDriverModule;
import io.harness.threading.ThreadPool;
import io.harness.time.TimeModule;
import io.harness.yaml.YamlSdkModule;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.schema.client.YamlSchemaClientModule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.dropwizard.jackson.Jackson;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@Slf4j
public class PipelineServiceModule extends AbstractModule {
  private final PipelineServiceConfiguration configuration;

  private static PipelineServiceModule instance;

  private PipelineServiceModule(PipelineServiceConfiguration configuration) {
    this.configuration = configuration;
  }

  public static PipelineServiceModule getInstance(PipelineServiceConfiguration appConfig) {
    if (instance == null) {
      instance = new PipelineServiceModule(appConfig);
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    install(PipelineServiceGrpcModule.getInstance());
    install(new PipelinePersistenceModule());
    install(PmsDelegateServiceDriverModule.getInstance());
    install(OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                                .serviceName("PIPELINE")
                                                .expressionEvaluatorProvider(new PMSExpressionEvaluatorProvider())
                                                .withPMS(false)
                                                .isPipelineService(true)
                                                .build()));
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
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
    install(OrchestrationVisualizationModule.getInstance());
    install(new DelegateServiceDriverGrpcClientModule(configuration.getManagerServiceSecret(),
        configuration.getManagerTarget(), configuration.getManagerAuthority()));
    install(new ConnectorResourceClientModule(configuration.getNgManagerServiceHttpClientConfig(),
        configuration.getNgManagerServiceSecret(), MANAGER.getServiceId()));
    install(new SecretManagementClientModule(
        configuration.getManagerClientConfig(), configuration.getManagerServiceSecret(), MANAGER.getServiceId()));
    install(NGTriggersModule.getInstance());
    install(PersistentLockModule.getInstance());
    install(TimeModule.getInstance());
    install(FiltersModule.getInstance());
    install(YamlSdkModule.getInstance());

    install(new OrganizationManagementClientModule(configuration.getNgManagerServiceHttpClientConfig(),
        configuration.getNgManagerServiceSecret(), PIPELINE_SERVICE.getServiceId()));
    install(new ProjectManagementClientModule(configuration.getNgManagerServiceHttpClientConfig(),
        configuration.getNgManagerServiceSecret(), PIPELINE_SERVICE.getServiceId()));
    install(
        YamlSchemaClientModule.getInstance(configuration.getYamlSchemaClientConfig(), PIPELINE_SERVICE.getServiceId()));
    install(new UserClientModule(configuration.getManagerClientConfig(), configuration.getManagerServiceSecret(),
        PIPELINE_SERVICE.getServiceId()));
    install(new AccountClientModule(configuration.getManagerClientConfig(), configuration.getManagerServiceSecret(),
        PIPELINE_SERVICE.getServiceId()));
    install(new DelegateSelectionLogHttpClientModule(configuration.getManagerClientConfig(),
        configuration.getManagerServiceSecret(), PIPELINE_SERVICE.getServiceId()));
    install(new EventsFrameworkModule(configuration.getEventsFrameworkConfiguration()));
    install(new EntitySetupUsageClientModule(this.configuration.getNgManagerServiceHttpClientConfig(),
        this.configuration.getManagerServiceSecret(), PIPELINE_SERVICE.getServiceId()));

    bind(HPersistence.class).to(MongoPersistence.class);
    bind(PMSPipelineService.class).to(PMSPipelineServiceImpl.class);
    bind(PMSInputSetService.class).to(PMSInputSetServiceImpl.class);
    bind(PMSExecutionService.class).to(PMSExecutionServiceImpl.class);
    bind(PMSYamlSchemaService.class).to(PMSYamlSchemaServiceImpl.class);

    bind(StepTypeLookupService.class).to(StepTypeLookupServiceImpl.class);
    bind(NodeTypeLookupService.class).to(NodeTypeLookupServiceImpl.class);

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("taskPollExecutor"))
        .toInstance(new ManagedScheduledExecutorService("TaskPoll-Thread"));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("progressUpdateServiceExecutor"))
        .toInstance(new ManagedScheduledExecutorService("ProgressUpdateServiceExecutor-Thread"));
    bind(TriggerWebhookExecutionService.class).to(TriggerWebhookExecutionServiceImpl.class);
    MapBinder<String, FilterPropertiesMapper> filterPropertiesMapper =
        MapBinder.newMapBinder(binder(), String.class, FilterPropertiesMapper.class);
    filterPropertiesMapper.addBinding(FilterType.PIPELINESETUP.toString()).to(PipelineFilterPropertiesMapper.class);
    filterPropertiesMapper.addBinding(FilterType.PIPELINEEXECUTION.toString())
        .to(PipelineExecutionFilterPropertiesMapper.class);

    bind(PMSBarrierService.class).to(PMSBarrierServiceImpl.class);
    bind(ApprovalResourceService.class).to(ApprovalResourceServiceImpl.class);
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.kryoRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public Set<Class<? extends TypeConverter>> morphiaConverters() {
    return ImmutableSet.<Class<? extends TypeConverter>>builder()
        .addAll(PipelineServiceModuleRegistrars.morphiaConverters)
        .build();
  }

  @Provides
  @Singleton
  List<Class<? extends Converter<?, ?>>> springConverters() {
    return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
        .addAll(PipelineServiceModuleRegistrars.springConverters)
        .build();
  }

  @Provides
  @Singleton
  List<YamlSchemaRootClass> yamlSchemaRootClasses() {
    return ImmutableList.<YamlSchemaRootClass>builder()
        .addAll(OrchestrationStepsModuleRegistrars.yamlSchemaRegistrars)
        .build();
  }

  @Provides
  @Singleton
  public MongoConfig mongoConfig(PipelineServiceConfiguration configuration) {
    return configuration.getMongoConfig();
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder()
        .put(DelegateSyncTaskResponse.class, "pms_delegateSyncTaskResponses")
        .put(DelegateAsyncTaskResponse.class, "pms_delegateAsyncTaskResponses")
        .put(DelegateTaskProgressResponse.class, "pms_delegateTaskProgressResponses")
        .build();
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return (Supplier<DelegateCallbackToken>) Suppliers.memoize(
        () -> getDelegateCallbackToken(delegateServiceGrpcClient));
  }

  private DelegateCallbackToken getDelegateCallbackToken(DelegateServiceGrpcClient delegateServiceClient) {
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("pms")
                                  .setConnection(configuration.getMongoConfig().getUri())
                                  .build())
            .build());
    log.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return MONGO;
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisConfig() {
    return RedisConfig.builder().build();
  }

  @Provides
  @Singleton
  @Named("templateRegistrationExecutorService")
  public ExecutorService templateRegistrationExecutionServiceThreadPool() {
    return ThreadPool.create(
        1, 1, 10, TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("TemplateRegistrationService-%d").build());
  }

  @Provides
  @Named("yaml-schema-mapper")
  @Singleton
  public ObjectMapper getYamlSchemaObjectMapper() {
    return Jackson.newObjectMapper();
  }
}
