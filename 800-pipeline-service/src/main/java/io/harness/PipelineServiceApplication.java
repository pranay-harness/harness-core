package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.PipelineServiceConfiguration.getResourceClasses;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.pms.async.plan.PlanNotifyEventConsumer.PMS_PLAN_CREATION;
import static io.harness.waiter.PmsNotifyEventListener.PMS_ORCHESTRATION;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.accesscontrol.NGAccessDeniedExceptionMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.CacheModule;
import io.harness.consumers.GraphUpdateRedisConsumer;
import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.delay.DelayEventListener;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.OrchestrationService;
import io.harness.engine.OrchestrationServiceImpl;
import io.harness.engine.events.NodeExecutionStatusUpdateEventHandler;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanExecutionServiceImpl;
import io.harness.engine.expressions.OrchestrationConstants;
import io.harness.engine.interrupts.OrchestrationEndInterruptHandler;
import io.harness.engine.timeouts.TimeoutInstanceRemover;
import io.harness.event.OrchestrationEndGraphHandler;
import io.harness.event.OrchestrationLogPublisher;
import io.harness.event.OrchestrationStartEventHandler;
import io.harness.exception.GeneralException;
import io.harness.execution.consumers.SdkResponseEventRedisConsumer;
import io.harness.gitsync.AbstractGitSyncSdkModule;
import io.harness.gitsync.GitSdkConfiguration;
import io.harness.gitsync.GitSyncEntitiesConfiguration;
import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.GitSyncSdkInitHelper;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.persistance.NoOpGitSyncSdkServiceImpl;
import io.harness.gitsync.persistance.testing.NoOpGitAwarePersistenceImpl;
import io.harness.govern.ProviderModule;
import io.harness.graph.stepDetail.PmsGraphStepDetailsServiceImpl;
import io.harness.graph.stepDetail.service.PmsGraphStepDetailsService;
import io.harness.health.HealthMonitor;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigrationSdkInitHelper;
import io.harness.migration.NGMigrationSdkModule;
import io.harness.migration.beans.NGMigrationConfiguration;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.notification.module.NotificationClientModule;
import io.harness.outbox.OutboxEventPollService;
import io.harness.plan.consumers.PartialPlanResponseRedisConsumer;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.approval.ApprovalInstanceExpirationJob;
import io.harness.pms.approval.ApprovalInstanceHandler;
import io.harness.pms.async.plan.PlanNotifyEventConsumer;
import io.harness.pms.async.plan.PlanNotifyEventPublisher;
import io.harness.pms.event.PMSEventConsumerService;
import io.harness.pms.events.base.PipelineEventConsumerController;
import io.harness.pms.inputset.gitsync.InputSetEntityGitSyncHelper;
import io.harness.pms.inputset.gitsync.InputSetYamlDTO;
import io.harness.pms.migration.PipelineCoreMigrationProvider;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.observers.InputSetValidationObserver;
import io.harness.pms.ngpipeline.inputset.observers.InputSetsDeleteObserver;
import io.harness.pms.notification.orchestration.handlers.NotificationInformHandler;
import io.harness.pms.notification.orchestration.handlers.PipelineStartNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.StageStartNotificationHandler;
import io.harness.pms.notification.orchestration.handlers.StageStatusUpdateNotificationEventHandler;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntityCrudObserver;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.gitsync.PipelineEntityGitSyncHelper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineServiceImpl;
import io.harness.pms.plan.creation.PipelineServiceFilterCreationResponseMerger;
import io.harness.pms.plan.creation.PipelineServiceInternalInfoProvider;
import io.harness.pms.plan.execution.PmsExecutionServiceInfoProvider;
import io.harness.pms.plan.execution.handlers.ExecutionInfoUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.ExecutionSummaryCreateEventHandler;
import io.harness.pms.plan.execution.handlers.ExecutionSummaryStatusUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.ExecutionSummaryUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.PipelineStatusUpdateEventHandler;
import io.harness.pms.plan.execution.handlers.PlanStatusEventEmitterHandler;
import io.harness.pms.plan.execution.observers.PipelineExecutionSummaryDeleteObserver;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkInitHelper;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.SdkDeployMode;
import io.harness.pms.sdk.execution.events.facilitators.FacilitatorEventRedisConsumer;
import io.harness.pms.sdk.execution.events.interrupts.InterruptEventRedisConsumer;
import io.harness.pms.sdk.execution.events.node.advise.NodeAdviseEventRedisConsumer;
import io.harness.pms.sdk.execution.events.node.resume.NodeResumeEventRedisConsumer;
import io.harness.pms.sdk.execution.events.node.start.NodeStartEventRedisConsumer;
import io.harness.pms.sdk.execution.events.orchestrationevent.OrchestrationEventRedisConsumer;
import io.harness.pms.sdk.execution.events.plan.CreatePartialPlanRedisConsumer;
import io.harness.pms.sdk.execution.events.progress.ProgressEventRedisConsumer;
import io.harness.pms.serializer.jackson.PmsBeansJacksonModule;
import io.harness.pms.triggers.scheduled.ScheduledTriggerHandler;
import io.harness.pms.triggers.webhook.service.TriggerWebhookExecutionService;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.registrars.PipelineServiceFacilitatorRegistrar;
import io.harness.registrars.PipelineServiceStepRegistrar;
import io.harness.request.RequestContextFilter;
import io.harness.resource.VersionInfoResource;
import io.harness.security.NextGenAuthenticationFilter;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.serializer.PipelineServiceUtilAdviserRegistrar;
import io.harness.serializer.jackson.PipelineServiceJacksonModule;
import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateProgressServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.service.impl.GraphGenerationServiceImpl;
import io.harness.springdata.HMongoTemplate;
import io.harness.steps.barriers.BarrierInitializer;
import io.harness.steps.barriers.event.BarrierDropper;
import io.harness.steps.barriers.event.BarrierPositionHelperEventHandler;
import io.harness.steps.barriers.service.BarrierServiceImpl;
import io.harness.steps.resourcerestraint.ResourceRestraintInitializer;
import io.harness.steps.resourcerestraint.service.ResourceRestraintPersistenceMonitor;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import io.harness.threading.ThreadPoolConfig;
import io.harness.timeout.TimeoutEngine;
import io.harness.token.remote.TokenClient;
import io.harness.tracing.MongoRedisTracer;
import io.harness.utils.NGObjectMapperHelper;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.PmsNotifyEventConsumerRedis;
import io.harness.waiter.PmsNotifyEventListener;
import io.harness.waiter.PmsNotifyEventPublisher;
import io.harness.waiter.ProgressUpdateService;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.YamlSdkInitHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@OwnedBy(PIPELINE)
public class PipelineServiceApplication extends Application<PipelineServiceConfiguration> {
  private static final SecureRandom random = new SecureRandom();
  private static final String APPLICATION_NAME = "Pipeline Service Application";

  private final MetricRegistry metricRegistry = new MetricRegistry();
  private HarnessMetricRegistry harnessMetricRegistry;

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new PipelineServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<PipelineServiceConfiguration> bootstrap) {
    initializeLogging();
    bootstrap.addCommand(new InspectCommand<>(this));

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
    bootstrap.addBundle(new SwaggerBundle<PipelineServiceConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(PipelineServiceConfiguration appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
    mapper.registerModule(new PmsBeansJacksonModule());
    mapper.registerModule(new PipelineServiceJacksonModule());
  }

  @Override
  public void run(PipelineServiceConfiguration appConfig, Environment environment) {
    log.info("Starting Pipeline Service Application ...");
    MaintenanceController.forceMaintenance(true);

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        10, 100, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      PipelineServiceConfiguration configuration() {
        return appConfig;
      }
    });
    modules.add(new NotificationClientModule(appConfig.getNotificationClientConfiguration()));
    modules.add(PipelineServiceModule.getInstance(appConfig));
    modules.add(new MetricRegistryModule(metricRegistry));
    modules.add(NGMigrationSdkModule.getInstance());
    CacheModule cacheModule = new CacheModule(appConfig.getCacheConfig());
    modules.add(cacheModule);
    if (appConfig.isShouldDeployWithGitSync()) {
      GitSyncSdkConfiguration gitSyncSdkConfiguration = getGitSyncConfiguration(appConfig);
      modules.add(new AbstractGitSyncSdkModule() {
        @Override
        public GitSyncSdkConfiguration getGitSyncSdkConfiguration() {
          return gitSyncSdkConfiguration;
        }
      });
    } else {
      modules.add(new SCMGrpcClientModule(appConfig.getGitSdkConfiguration().getScmConnectionConfig()));
      modules.add(new AbstractGitSyncSdkModule() {
        @Override
        protected void configure() {
          bind(GitAwarePersistence.class).to(NoOpGitAwarePersistenceImpl.class);
          bind(GitSyncSdkService.class).to(NoOpGitSyncSdkServiceImpl.class);
        }

        @Override
        public GitSyncSdkConfiguration getGitSyncSdkConfiguration() {
          return null;
        }
      });
    }

    // Pipeline Service Modules
    PmsSdkConfiguration pmsSdkConfiguration = getPmsSdkConfiguration(appConfig);
    modules.add(PmsSdkModule.getInstance(pmsSdkConfiguration));
    modules.add(PipelineServiceUtilityModule.getInstance());

    Injector injector = Guice.createInjector(modules);
    registerEventListeners(injector);
    registerWaitEnginePublishers(injector);
    registerScheduledJobs(injector, appConfig);
    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment, injector);
    registerManagedBeans(environment, injector);
    registerAuthFilters(appConfig, environment, injector);
    registerHealthCheck(environment, injector);
    registerObservers(injector);
    registerRequestContextFilter(environment);

    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);
    injector.getInstance(TriggerWebhookExecutionService.class).registerIterators();
    injector.getInstance(ScheduledTriggerHandler.class).registerIterators();
    injector.getInstance(TimeoutEngine.class).registerIterators();
    injector.getInstance(BarrierServiceImpl.class).registerIterators();
    injector.getInstance(ApprovalInstanceHandler.class).registerIterators();
    injector.getInstance(ResourceRestraintPersistenceMonitor.class).registerIterators();
    injector.getInstance(PrimaryVersionChangeScheduler.class).registerExecutors();

    log.info("Initializing gRPC servers...");
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    registerPmsSdk(appConfig, injector);
    registerYamlSdk(injector);
    if (appConfig.isShouldDeployWithGitSync()) {
      registerGitSyncSdk(appConfig, injector, environment);
    }

    registerCorrelationFilter(environment, injector);
    registerNotificationTemplates(injector);
    registerPmsSdkEvents(injector);
    registerMigrations(injector);
    MaintenanceController.forceMaintenance(false);
  }

  private static void registerObservers(Injector injector) {
    PmsGraphStepDetailsServiceImpl pmsGraphStepDetailsService =
        (PmsGraphStepDetailsServiceImpl) injector.getInstance(Key.get(PmsGraphStepDetailsService.class));
    pmsGraphStepDetailsService.getStepDetailsUpdateObserverSubject().register(
        injector.getInstance(Key.get(OrchestrationLogPublisher.class)));

    // Register Pipeline Observers
    PMSPipelineServiceImpl pmsPipelineService =
        (PMSPipelineServiceImpl) injector.getInstance(Key.get(PMSPipelineService.class));
    pmsPipelineService.getPipelineSubject().register(injector.getInstance(Key.get(PipelineSetupUsageHelper.class)));
    pmsPipelineService.getPipelineSubject().register(injector.getInstance(Key.get(PipelineEntityCrudObserver.class)));
    pmsPipelineService.getPipelineSubject().register(
        injector.getInstance(Key.get(PipelineExecutionSummaryDeleteObserver.class)));
    pmsPipelineService.getPipelineSubject().register(injector.getInstance(Key.get(InputSetsDeleteObserver.class)));
    pmsPipelineService.getPipelineSubject().register(injector.getInstance(Key.get(InputSetValidationObserver.class)));

    NodeExecutionServiceImpl nodeExecutionService =
        (NodeExecutionServiceImpl) injector.getInstance(Key.get(NodeExecutionService.class));

    // NodeStatusUpdateObserver
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(PlanExecutionService.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(StageStatusUpdateNotificationEventHandler.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(BarrierPositionHelperEventHandler.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(injector.getInstance(Key.get(BarrierDropper.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(NodeExecutionStatusUpdateEventHandler.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(OrchestrationLogPublisher.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(ExecutionSummaryStatusUpdateEventHandler.class)));
    nodeExecutionService.getStepStatusUpdateSubject().register(
        injector.getInstance(Key.get(TimeoutInstanceRemover.class)));

    // NodeUpdateObservers
    nodeExecutionService.getNodeUpdateObserverSubject().register(
        injector.getInstance(Key.get(ExecutionSummaryUpdateEventHandler.class)));
    nodeExecutionService.getNodeUpdateObserverSubject().register(
        injector.getInstance(Key.get(OrchestrationLogPublisher.class)));
    nodeExecutionService.getNodeUpdateObserverSubject().register(
        injector.getInstance(Key.get(OrchestrationLogPublisher.class)));

    // NodeExecutionStartObserver
    nodeExecutionService.getNodeExecutionStartSubject().register(
        injector.getInstance(Key.get(StageStartNotificationHandler.class)));

    PlanStatusEventEmitterHandler planStatusEventEmitterHandler =
        injector.getInstance(Key.get(PlanStatusEventEmitterHandler.class));
    planStatusEventEmitterHandler.getPlanExecutionSubject().register(
        injector.getInstance(Key.get(NotificationInformHandler.class)));

    PlanExecutionServiceImpl planExecutionService =
        (PlanExecutionServiceImpl) injector.getInstance(Key.get(PlanExecutionService.class));
    planExecutionService.getPlanStatusUpdateSubject().register(
        injector.getInstance(Key.get(ExecutionInfoUpdateEventHandler.class)));
    planExecutionService.getPlanStatusUpdateSubject().register(planStatusEventEmitterHandler);
    planExecutionService.getPlanStatusUpdateSubject().register(
        injector.getInstance(Key.get(PipelineStatusUpdateEventHandler.class)));
    planExecutionService.getPlanStatusUpdateSubject().register(
        injector.getInstance(Key.get(OrchestrationLogPublisher.class)));

    OrchestrationServiceImpl orchestrationService =
        (OrchestrationServiceImpl) injector.getInstance(Key.get(OrchestrationService.class));
    orchestrationService.getOrchestrationStartSubject().register(
        injector.getInstance(Key.get(PipelineStartNotificationHandler.class)));
    orchestrationService.getOrchestrationStartSubject().register(
        injector.getInstance(Key.get(BarrierInitializer.class)));
    orchestrationService.getOrchestrationStartSubject().register(
        injector.getInstance(Key.get(ResourceRestraintInitializer.class)));
    orchestrationService.getOrchestrationStartSubject().register(
        injector.getInstance(Key.get(OrchestrationStartEventHandler.class)));
    orchestrationService.getOrchestrationStartSubject().register(
        injector.getInstance(Key.get(ExecutionSummaryCreateEventHandler.class)));

    OrchestrationEngine orchestrationEngine = injector.getInstance(Key.get(OrchestrationEngine.class));
    orchestrationEngine.getOrchestrationEndSubject().register(
        injector.getInstance(Key.get(OrchestrationEndGraphHandler.class)));
    orchestrationEngine.getOrchestrationEndSubject().register(
        injector.getInstance(Key.get(OrchestrationEndInterruptHandler.class)));
    orchestrationEngine.getOrchestrationEndSubject().register(
        injector.getInstance(Key.get(NotificationInformHandler.class)));
    GraphGenerationServiceImpl graphGenerationService =
        (GraphGenerationServiceImpl) injector.getInstance(Key.get(GraphGenerationServiceImpl.class));
    graphGenerationService.getGraphNodeUpdateObserverSubject().register(
        injector.getInstance(Key.get(ExecutionSummaryStatusUpdateEventHandler.class)));

    HMongoTemplate mongoTemplate = (HMongoTemplate) injector.getInstance(MongoTemplate.class);
    mongoTemplate.getTracerSubject().register(injector.getInstance(MongoRedisTracer.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }

  private void registerAuthFilters(PipelineServiceConfiguration config, Environment environment, Injector injector) {
    Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate = resourceInfoAndRequest
        -> resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(PipelineServiceAuth.class) != null
        || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(PipelineServiceAuth.class) != null
        || resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(NextGenManagerAuth.class) != null
        || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(NextGenManagerAuth.class) != null;
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(AuthorizationServiceHeader.BEARER.getServiceId(), config.getJwtAuthSecret());
    serviceToSecretMapping.put(
        AuthorizationServiceHeader.IDENTITY_SERVICE.getServiceId(), config.getJwtIdentityServiceSecret());
    serviceToSecretMapping.put(AuthorizationServiceHeader.DEFAULT.getServiceId(), config.getNgManagerServiceSecret());
    environment.jersey().register(new NextGenAuthenticationFilter(predicate, null, serviceToSecretMapping,
        injector.getInstance(Key.get(TokenClient.class, Names.named("PRIVILEGED")))));
  }

  private void registerHealthCheck(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("PMS", healthService);
    healthService.registerMonitor((HealthMonitor) injector.getInstance(MongoTemplate.class));
  }

  private void registerPmsSdk(PipelineServiceConfiguration config, Injector injector) {
    PmsSdkConfiguration sdkConfig = getPmsSdkConfiguration(config);
    try {
      PmsSdkInitHelper.initializeSDKInstance(injector, sdkConfig);
    } catch (Exception ex) {
      throw new GeneralException("Failed to start pipeline service because pms sdk registration failed", ex);
    }
  }

  private PmsSdkConfiguration getPmsSdkConfiguration(PipelineServiceConfiguration config) {
    return PmsSdkConfiguration.builder()
        .deploymentMode(SdkDeployMode.REMOTE_IN_PROCESS)
        .moduleType(ModuleType.PMS)
        .pipelineServiceInfoProviderClass(PipelineServiceInternalInfoProvider.class)
        .filterCreationResponseMerger(new PipelineServiceFilterCreationResponseMerger())
        .engineSteps(PipelineServiceStepRegistrar.getEngineSteps())
        .engineFacilitators(PipelineServiceFacilitatorRegistrar.getEngineFacilitators())
        .engineAdvisers(PipelineServiceUtilAdviserRegistrar.getEngineAdvisers())
        .staticAliases(getStaticAliases())
        .engineEventHandlersMap(of())
        .executionSummaryModuleInfoProviderClass(PmsExecutionServiceInfoProvider.class)
        .eventsFrameworkConfiguration(config.getEventsFrameworkConfiguration())
        .executionPoolConfig(ThreadPoolConfig.builder().corePoolSize(20).maxPoolSize(100).idleTime(120L).build())
        .orchestrationEventPoolConfig(
            ThreadPoolConfig.builder().corePoolSize(10).maxPoolSize(50).idleTime(120L).build())
        .build();
  }

  @VisibleForTesting
  public Map<String, String> getStaticAliases() {
    Map<String, String> aliases = new HashMap<>();
    aliases.put(OrchestrationConstants.STAGE_SUCCESS,
        "<+stage.currentStatus> == \"SUCCEEDED\" || <+stage.currentStatus> == \"IGNORE_FAILED\"");
    aliases.put(OrchestrationConstants.STAGE_FAILURE,
        "<+stage.currentStatus> == \"FAILED\" || <+stage.currentStatus> == \"ERRORED\" || <+stage.currentStatus> == \"EXPIRED\"");
    aliases.put(OrchestrationConstants.PIPELINE_FAILURE,
        "<+pipeline.currentStatus> == \"FAILED\" || <+pipeline.currentStatus> == \"ERRORED\" || <+pipeline.currentStatus> == \"EXPIRED\"");
    aliases.put(OrchestrationConstants.PIPELINE_SUCCESS,
        "<+pipeline.currentStatus> == \"SUCCEEDED\" || <+pipeline.currentStatus> == \"IGNORE_FAILED\"");
    aliases.put(OrchestrationConstants.ALWAYS, "true");
    return aliases;
  }

  private void registerGitSyncSdk(PipelineServiceConfiguration config, Injector injector, Environment environment) {
    GitSyncSdkConfiguration sdkConfig = getGitSyncConfiguration(config);
    try {
      GitSyncSdkInitHelper.initGitSyncSdk(injector, environment, sdkConfig);
    } catch (Exception ex) {
      throw new GeneralException("Failed to start pipeline service because git sync registration failed", ex);
    }
  }

  private GitSyncSdkConfiguration getGitSyncConfiguration(PipelineServiceConfiguration config) {
    final Supplier<List<EntityType>> sortOrder = () -> Lists.newArrayList(EntityType.PIPELINES, EntityType.INPUT_SETS);
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    configureObjectMapper(objectMapper);
    Set<GitSyncEntitiesConfiguration> gitSyncEntitiesConfigurations = new HashSet<>();
    gitSyncEntitiesConfigurations.add(GitSyncEntitiesConfiguration.builder()
                                          .yamlClass(PipelineConfig.class)
                                          .entityClass(PipelineEntity.class)
                                          .entityType(EntityType.PIPELINES)
                                          .entityHelperClass(PipelineEntityGitSyncHelper.class)
                                          .build());
    gitSyncEntitiesConfigurations.add(GitSyncEntitiesConfiguration.builder()
                                          .yamlClass(InputSetYamlDTO.class)
                                          .entityClass(InputSetEntity.class)
                                          .entityType(EntityType.INPUT_SETS)
                                          .entityHelperClass(InputSetEntityGitSyncHelper.class)
                                          .build());
    final GitSdkConfiguration gitSdkConfiguration = config.getGitSdkConfiguration();
    return GitSyncSdkConfiguration.builder()
        .gitSyncSortOrder(sortOrder)
        .grpcClientConfig(gitSdkConfiguration.getGitManagerGrpcClientConfig())
        .grpcServerConfig(gitSdkConfiguration.getGitSdkGrpcServerConfig())
        .deployMode(GitSyncSdkConfiguration.DeployMode.REMOTE)
        .microservice(Microservice.PMS)
        .scmConnectionConfig(gitSdkConfiguration.getScmConnectionConfig())
        .eventsRedisConfig(config.getEventsFrameworkConfiguration().getRedisConfig())
        .serviceHeader(PIPELINE_SERVICE)
        .gitSyncEntitiesConfiguration(gitSyncEntitiesConfigurations)
        .objectMapper(objectMapper)
        .build();
  }

  private void registerEventListeners(Injector injector) {
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(DelayEventListener.class), 1);
    queueListenerController.register(injector.getInstance(PmsNotifyEventListener.class), 3);
  }

  private void registerWaitEnginePublishers(Injector injector) {
    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(PMS_ORCHESTRATION, injector.getInstance(PmsNotifyEventPublisher.class));
    notifyQueuePublisherRegister.register(PMS_PLAN_CREATION, injector.getInstance(PlanNotifyEventPublisher.class));
  }

  private void registerScheduledJobs(Injector injector, PipelineServiceConfiguration appConfig) {
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateSyncServiceImpl.class), 0L,
            appConfig.getDelegatePollingConfig().getSyncDelay(), TimeUnit.MILLISECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateAsyncServiceImpl.class), 0L,
            appConfig.getDelegatePollingConfig().getAsyncDelay(), TimeUnit.MILLISECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateProgressServiceImpl.class), 0L,
            appConfig.getDelegatePollingConfig().getProgressDelay(), TimeUnit.MILLISECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("progressUpdateServiceExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(ProgressUpdateService.class), 0L,
            appConfig.getDelegatePollingConfig().getProgressDelay(), TimeUnit.MILLISECONDS);

    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), random.nextInt(200), 200L, TimeUnit.SECONDS);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(PMSEventConsumerService.class));
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(ApprovalInstanceExpirationJob.class));
    environment.lifecycle().manage(injector.getInstance(OutboxEventPollService.class));
    environment.lifecycle().manage(injector.getInstance(PipelineEventConsumerController.class));
  }

  private void registerPmsSdkEvents(Injector injector) {
    log.info("Initializing pms sdk redis abstract consumers...");
    PipelineEventConsumerController pipelineEventConsumerController =
        injector.getInstance(PipelineEventConsumerController.class);
    pipelineEventConsumerController.register(injector.getInstance(InterruptEventRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(OrchestrationEventRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(FacilitatorEventRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(NodeStartEventRedisConsumer.class), 2);
    pipelineEventConsumerController.register(injector.getInstance(ProgressEventRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(NodeAdviseEventRedisConsumer.class), 2);
    pipelineEventConsumerController.register(injector.getInstance(NodeResumeEventRedisConsumer.class), 2);
    pipelineEventConsumerController.register(injector.getInstance(SdkResponseEventRedisConsumer.class), 3);
    pipelineEventConsumerController.register(injector.getInstance(GraphUpdateRedisConsumer.class), 3);
    pipelineEventConsumerController.register(injector.getInstance(PartialPlanResponseRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(CreatePartialPlanRedisConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(PlanNotifyEventConsumer.class), 1);
    pipelineEventConsumerController.register(injector.getInstance(PmsNotifyEventConsumerRedis.class), 2);
  }

  private void registerCorsFilter(PipelineServiceConfiguration appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
    environment.jersey().register(injector.getInstance(VersionInfoResource.class));
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(NGAccessDeniedExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapperV2.class);

    environment.jersey().register(MultiPartFeature.class);
    //    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
    //    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
    //    environment.jersey().register(injector.getInstance(EtagFilter.class));
  }

  private void registerYamlSdk(Injector injector) {
    YamlSdkConfiguration yamlSdkConfiguration = YamlSdkConfiguration.builder()
                                                    .requireSchemaInit(true)
                                                    .requireSnippetInit(true)
                                                    .requireValidatorInit(false)
                                                    .build();
    YamlSdkInitHelper.initialize(injector, yamlSdkConfiguration);
  }

  private void registerNotificationTemplates(Injector injector) {
    ExecutorService executorService =
        injector.getInstance(Key.get(ExecutorService.class, Names.named("templateRegistrationExecutorService")));
    executorService.submit(injector.getInstance(NotificationTemplateRegistrar.class));
  }

  private void registerRequestContextFilter(Environment environment) {
    environment.jersey().register(new RequestContextFilter());
  }

  private void registerMigrations(Injector injector) {
    NGMigrationConfiguration config = getMigrationSdkConfiguration();
    NGMigrationSdkInitHelper.initialize(injector, config);
  }

  private NGMigrationConfiguration getMigrationSdkConfiguration() {
    return NGMigrationConfiguration.builder()
        .microservice(Microservice.PMS)
        .migrationProviderList(new ArrayList<Class<? extends MigrationProvider>>() {
          { add(PipelineCoreMigrationProvider.class); } // Add all migration provider classes here
        })
        .build();
  }
}
