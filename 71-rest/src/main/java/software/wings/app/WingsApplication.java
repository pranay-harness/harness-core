package software.wings.app;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.inject.matcher.Matchers.not;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.lock.PersistentLocker.LOCKS_STORE;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.FeatureName.GLOBAL_DISABLE_HEALTH_CHECK;
import static software.wings.beans.FeatureName.PERPETUAL_TASK_SERVICE;
import static software.wings.common.VerificationConstants.CV_24X7_METRIC_LABELS;
import static software.wings.common.VerificationConstants.CV_META_DATA;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;
import static software.wings.common.VerificationConstants.VERIFICATION_METRIC_LABELS;
import static software.wings.utils.CacheManager.USER_CACHE;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.hazelcast.core.HazelcastInstance;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.event.EventsModule;
import io.harness.event.listener.EventListener;
import io.harness.event.reconciliation.service.DeploymentReconExecutorService;
import io.harness.event.reconciliation.service.DeploymentReconTask;
import io.harness.event.usagemetrics.EventsModuleHelper;
import io.harness.exception.WingsException;
import io.harness.govern.ProviderModule;
import io.harness.grpc.GrpcServerConfig;
import io.harness.grpc.GrpcServiceConfigurationModule;
import io.harness.health.HealthService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.lock.AcquiredLock;
import io.harness.lock.ManageDistributedLockSvc;
import io.harness.lock.PersistentLocker;
import io.harness.maintenance.HazelcastListener;
import io.harness.maintenance.MaintenanceController;
import io.harness.marketplace.gcp.GcpMarketplaceSubscriberService;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.perpetualtask.internal.PerpetualTaskRecordHandler;
import io.harness.perpetualtask.internal.RecentlyDisconnectedDelegateHandler;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.scheduler.PersistentScheduler;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import io.harness.waiter.Notifier;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.collect.ArtifactCollectEventListener;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.filter.AuditRequestFilter;
import software.wings.filter.AuditResponseFilter;
import software.wings.jersey.JsonViews;
import software.wings.jersey.KryoFeature;
import software.wings.licensing.LicenseService;
import software.wings.notification.EmailNotificationListener;
import software.wings.prune.PruneEntityListener;
import software.wings.resources.AppResource;
import software.wings.scheduler.AccountIdAdditionJob;
import software.wings.scheduler.AdministrativeJob;
import software.wings.scheduler.ExecutionLogsPruneJob;
import software.wings.scheduler.InstancesPurgeJob;
import software.wings.scheduler.LicenseCheckJob;
import software.wings.scheduler.ResourceConstraintBackupJob;
import software.wings.scheduler.UsageMetricsJob;
import software.wings.scheduler.WorkflowExecutionMonitorJob;
import software.wings.scheduler.YamlChangeSetPruneJob;
import software.wings.scheduler.ZombieHunterJob;
import software.wings.scheduler.approval.ApprovalPollingHandler;
import software.wings.scheduler.artifact.ArtifactCleanupHandler;
import software.wings.scheduler.artifact.ArtifactCollectionHandler;
import software.wings.scheduler.audit.EntityAuditRecordHandler;
import software.wings.scheduler.ecs.ECSPollingHandler;
import software.wings.scheduler.events.segment.SegmentGroupEventJob.SegmentGroupEventJobExecutor;
import software.wings.scheduler.instance.InstanceSyncHandler;
import software.wings.scheduler.marketplace.gcp.GCPBillingHandler;
import software.wings.scheduler.persistance.PersistentLockCleanup;
import software.wings.search.framework.ElasticsearchSyncService;
import software.wings.security.AuthResponseFilter;
import software.wings.security.AuthRuleFilter;
import software.wings.security.AuthenticationFilter;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.BarrierServiceImpl;
import software.wings.service.impl.DelayEventListener;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.ExecutionEventListener;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.event.DeploymentTimeSeriesEventListener;
import software.wings.service.impl.instance.DeploymentEventListener;
import software.wings.service.impl.instance.InstanceEventListener;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.impl.trigger.ScheduleTriggerHandler;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.impl.yaml.YamlPushServiceImpl;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateMachineExecutor;
import software.wings.utils.CacheManager;
import software.wings.yaml.gitSync.GitChangeSetRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

/**
 * The main application - entry point for the entire Wings Application.
 *
 * @author Rishi
 */
@Slf4j
public class WingsApplication extends Application<MainConfiguration> {
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private HarnessMetricRegistry harnessMetricRegistry;

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new WingsApplication().run(args);
  }

  @Override
  public String getName() {
    return "Wings Application";
  }

  @Override
  public void initialize(Bootstrap<MainConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<MainConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(MainConfiguration mainConfiguration) {
        return mainConfiguration.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    configureObjectMapper(bootstrap.getObjectMapper());
    bootstrap.setMetricRegistry(metricRegistry);

    logger.info("bootstrapping done.");
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    mapper.addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    mapper.setSubtypeResolver(new JsonSubtypeResolver(mapper.getSubtypeResolver()));
    mapper.setConfig(mapper.getSerializationConfig().withView(JsonViews.Public.class));
  }

  @Override
  public void run(final MainConfiguration configuration, Environment environment) throws Exception {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(20, 1000, 500L, TimeUnit.MILLISECONDS));

    List<Module> modules = new ArrayList<>();

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });

    modules.addAll(new MongoModule().cumulativeDependencies());

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration);
    modules.add(cacheModule);
    StreamModule streamModule = new StreamModule(environment, cacheModule.getHazelcastInstance());

    modules.add(streamModule);

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HazelcastInstance.class).toInstance(cacheModule.getHazelcastInstance());
        bind(MetricRegistry.class).toInstance(metricRegistry);
      }
    });

    modules.add(MetricsInstrumentationModule.builder()
                    .withMetricRegistry(metricRegistry)
                    .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
                      @Override
                      public boolean matches(TypeLiteral<?> typeLiteral) {
                        return typeLiteral.getRawType().isAnnotationPresent(Path.class);
                      }
                    }))
                    .build());

    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerQueueModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new MetricRegistryModule(metricRegistry));
    modules.add(new EventsModule(configuration));
    modules.add(new GraphQLModule());
    modules.add(new SSOModule());
    modules.add(new SignupModule());
    modules.add(new AuthModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    modules.add(new SearchModule());
    modules.add(new ProviderModule() {
      @Provides
      public GrpcServerConfig getGrpcServerConfig() {
        return configuration.getGrpcServerConfig();
      }
    });
    modules.add(new GrpcServiceConfigurationModule(configuration.getGrpcServerConfig()));

    Injector injector = Guice.createInjector(modules);

    Caching.getCachingProvider().getCacheManager().createCache(USER_CACHE, new Configuration<String, User>() {
      public static final long serialVersionUID = 1L;

      @Override
      public Class<String> getKeyType() {
        return String.class;
      }

      @Override
      public Class<User> getValueType() {
        return User.class;
      }

      @Override
      public boolean isStoreByValue() {
        return true;
      }
    });

    streamModule.getAtmosphereServlet().framework().objectFactory(new GuiceObjectFactory(injector));

    initializeFeatureFlags(injector);

    registerHealthChecks(environment, injector);

    registerStores(configuration, injector);

    registerResources(environment, injector);

    registerManagedBeans(configuration, environment, injector);

    registerQueueListeners(injector);

    scheduleJobs(injector);

    registerObservers(injector);

    registerCronJobs(injector);

    registerCorsFilter(configuration, environment);

    registerAuditResponseFilter(environment, injector);

    registerJerseyProviders(environment);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(configuration, environment, injector);

    // Register collection iterators
    if (configuration.isEnableIterators()) {
      registerIterators(injector);
    }

    environment.lifecycle().addServerLifecycleListener(server -> {
      for (Connector connector : server.getConnectors()) {
        if (connector instanceof ServerConnector) {
          ServerConnector serverConnector = (ServerConnector) connector;
          if (serverConnector.getName().equalsIgnoreCase("application")) {
            configuration.setSslEnabled(
                serverConnector.getDefaultConnectionFactory().getProtocol().equalsIgnoreCase("ssl"));
            configuration.setApplicationPort(serverConnector.getLocalPort());
            return;
          }
        }
      }
    });

    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);

    initMetrics();

    initializeFeatureFlags(injector);

    initializeServiceSecretKeys(injector);

    runMigrations(injector);

    // Access all caches before coming out of maintenance
    CacheManager cacheManager = injector.getInstance(CacheManager.class);

    cacheManager.getUserCache();
    cacheManager.getUserPermissionInfoCache();
    cacheManager.getUserRestrictionInfoCache();
    cacheManager.getApiKeyPermissionInfoCache();
    cacheManager.getApiKeyRestrictionInfoCache();
    cacheManager.getNewRelicApplicationCache();
    cacheManager.getWhitelistConfigCache();

    String deployMode = configuration.getDeployMode().name();

    if (DeployMode.isOnPrem(deployMode)) {
      LicenseService licenseService = injector.getInstance(LicenseService.class);
      String encryptedLicenseInfoBase64String = System.getenv(LicenseService.LICENSE_INFO);
      logger.info("Encrypted license info read from environment {}", encryptedLicenseInfoBase64String);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        logger.error("No license info is provided");
      } else {
        try {
          logger.info("Updating license info read from environment {}", encryptedLicenseInfoBase64String);
          licenseService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
          logger.info("Updated license info read from environment {}", encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          logger.error("Error while updating license info", ex);
        }
      }
    }

    injector.getInstance(EventsModuleHelper.class).initialize();
    if (injector.getInstance(FeatureFlagService.class).isGlobalEnabled(PERPETUAL_TASK_SERVICE)) {
      logger.info("Initializing gRPC server...");
      ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
      serviceManager.awaitHealthy();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
    }
    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");
  }

  private void initMetrics() {
    harnessMetricRegistry.registerCounterMetric(
        VERIFICATION_DEPLOYMENTS, VERIFICATION_METRIC_LABELS.toArray(new String[0]), " ");
    harnessMetricRegistry.registerGaugeMetric(CV_META_DATA, CV_24X7_METRIC_LABELS, " ");
  }

  private void initializeFeatureFlags(Injector injector) {
    injector.getInstance(FeatureFlagService.class).initializeFeatureFlags();
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("WingsApp", healthService);

    if (!injector.getInstance(FeatureFlagService.class).isGlobalEnabled(GLOBAL_DISABLE_HEALTH_CHECK)) {
      healthService.registerMonitor(injector.getInstance(HPersistence.class));
      healthService.registerMonitor(injector.getInstance(PersistentLocker.class));
    }
  }

  private void registerStores(MainConfiguration configuration, Injector injector) {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    if (isNotEmpty(configuration.getMongoConnectionFactory().getLocksUri())
        && !configuration.getMongoConnectionFactory().getLocksUri().equals(
               configuration.getMongoConnectionFactory().getUri())) {
      Set<Class> classes = ImmutableSet.<Class>of();

      persistence.register(LOCKS_STORE, configuration.getMongoConnectionFactory().getLocksUri(), classes);
    }
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  private void registerAuditResponseFilter(Environment environment, Injector injector) {
    environment.servlets()
        .addFilter("AuditResponseFilter", injector.getInstance(AuditResponseFilter.class))
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.jersey().register(injector.getInstance(AuditRequestFilter.class));
  }

  private void registerCorsFilter(MainConfiguration configuration, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = configuration.getPortal().getUrl();
    if (!configuration.getPortal().getAllowedOrigins().isEmpty()) {
      allowedOrigins = configuration.getPortal().getAllowedOrigins();
    }
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(AppResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerManagedBeans(MainConfiguration configuration, Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage(new ManageDistributedLockSvc(injector.getInstance(DistributedLockSvc.class)));
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
    environment.lifecycle().manage(injector.getInstance(ConfigurationController.class));
    environment.lifecycle().manage(injector.getInstance(TimerScheduledExecutorService.class));
    environment.lifecycle().manage(injector.getInstance(NotifierScheduledExecutorService.class));
    environment.lifecycle().manage(injector.getInstance(GcpMarketplaceSubscriberService.class));
    environment.lifecycle().manage((Managed) injector.getInstance(ExecutorService.class));
    if (configuration.isSearchEnabled()) {
      environment.lifecycle().manage(injector.getInstance(ElasticsearchSyncService.class));
    }
  }

  private void registerQueueListeners(Injector injector) {
    logger.info("Initializing queue listeners...");

    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    EventListener genericEventListener =
        injector.getInstance(Key.get(EventListener.class, Names.named("GenericEventListener")));
    queueListenerController.register((QueueListener) genericEventListener, 1);

    queueListenerController.register(injector.getInstance(ArtifactCollectEventListener.class), 1);
    queueListenerController.register(injector.getInstance(DelayEventListener.class), 1);
    queueListenerController.register(injector.getInstance(DeploymentEventListener.class), 2);
    queueListenerController.register(injector.getInstance(InstanceEventListener.class), 2);
    queueListenerController.register(injector.getInstance(DeploymentTimeSeriesEventListener.class), 2);
    queueListenerController.register(injector.getInstance(EmailNotificationListener.class), 1);
    queueListenerController.register(injector.getInstance(ExecutionEventListener.class), 3);
    queueListenerController.register(injector.getInstance(KmsTransitionEventListener.class), 1);
    queueListenerController.register(injector.getInstance(NotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(PruneEntityListener.class), 1);
  }

  private void scheduleJobs(Injector injector) {
    final Random rand = new Random();

    logger.info("Initializing scheduled jobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), rand.nextInt(10), 30L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("delegateTaskNotifier")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateQueueTask.class), rand.nextInt(5), 5L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("gitChangeSet")))
        .scheduleWithFixedDelay(
            injector.getInstance(GitChangeSetRunnable.class), rand.nextInt(4), 4L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(
            injector.getInstance(PersistentLockCleanup.class), rand.nextInt(60), 60L, TimeUnit.MINUTES);
    injector.getInstance(DeploymentReconExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(DeploymentReconTask.class), rand.nextInt(60), 15 * 60L, TimeUnit.SECONDS);
  }

  public static void registerObservers(Injector injector) {
    // Register Audit observer
    YamlPushServiceImpl yamlPushService = (YamlPushServiceImpl) injector.getInstance(Key.get(YamlPushService.class));
    AuditServiceImpl auditService = (AuditServiceImpl) injector.getInstance(Key.get(AuditService.class));
    yamlPushService.getEntityCrudSubject().register(auditService);

    AuditServiceHelper auditServiceHelper =
        (AuditServiceHelper) injector.getInstance(Key.get(AuditServiceHelper.class));
    auditServiceHelper.getEntityCrudSubject().register(auditService);

    registerSharedObservers(injector);
  }

  public static void registerSharedObservers(Injector injector) {
    final MaintenanceController maintenanceController = injector.getInstance(MaintenanceController.class);
    maintenanceController.register(new HazelcastListener());

    SettingsServiceImpl settingsService = (SettingsServiceImpl) injector.getInstance(Key.get(SettingsService.class));
    StateInspectionServiceImpl stateInspectionService =
        (StateInspectionServiceImpl) injector.getInstance(Key.get(StateInspectionService.class));
    StateMachineExecutor stateMachineExecutor = injector.getInstance(Key.get(StateMachineExecutor.class));
    WorkflowExecutionServiceImpl workflowExecutionService =
        (WorkflowExecutionServiceImpl) injector.getInstance(Key.get(WorkflowExecutionService.class));
    WorkflowServiceImpl workflowService = (WorkflowServiceImpl) injector.getInstance(Key.get(WorkflowService.class));

    settingsService.getManipulationSubject().register(workflowService);
    stateMachineExecutor.getStatusUpdateSubject().register(workflowExecutionService);
    stateInspectionService.getSubject().register(stateMachineExecutor);
  }

  public static void registerIterators(Injector injector) {
    final ScheduledThreadPoolExecutor artifactCollectionExecutor = new ScheduledThreadPoolExecutor(
        25, new ThreadFactoryBuilder().setNameFormat("Iterator-ArtifactCollection").build());

    final ArtifactCollectionHandler artifactCollectionHandler = new ArtifactCollectionHandler();
    injector.injectMembers(artifactCollectionHandler);

    PersistenceIterator artifactCollectionIterator = MongoPersistenceIterator.<ArtifactStream>builder()
                                                         .clazz(ArtifactStream.class)
                                                         .fieldName(ArtifactStreamKeys.nextIteration)
                                                         .targetInterval(ofMinutes(1))
                                                         .acceptableNoAlertDelay(ofSeconds(30))
                                                         .executorService(artifactCollectionExecutor)
                                                         .semaphore(new Semaphore(25))
                                                         .handler(artifactCollectionHandler)
                                                         .schedulingType(REGULAR)
                                                         .redistribute(true)
                                                         .build();

    injector.injectMembers(artifactCollectionIterator);
    artifactCollectionExecutor.scheduleAtFixedRate(
        () -> artifactCollectionIterator.process(ProcessMode.PUMP), 0, 10, TimeUnit.SECONDS);

    final ArtifactCleanupHandler artifactCleanupHandler = new ArtifactCleanupHandler();
    injector.injectMembers(artifactCleanupHandler);

    PersistenceIterator artifactCleanupIterator =
        MongoPersistenceIterator.<ArtifactStream>builder()
            .clazz(ArtifactStream.class)
            .fieldName(ArtifactStreamKeys.nextCleanupIteration)
            .targetInterval(ofHours(2))
            .acceptableNoAlertDelay(ofMinutes(15))
            .executorService(artifactCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(artifactCleanupHandler)
            .filterExpander(
                query -> query.filter(ArtifactStreamKeys.artifactStreamType, ArtifactStreamType.DOCKER.name()))
            .schedulingType(REGULAR)
            .redistribute(true)
            .build();

    injector.injectMembers(artifactCleanupIterator);
    artifactCollectionExecutor.scheduleAtFixedRate(
        () -> artifactCleanupIterator.process(ProcessMode.PUMP), 0, 5, TimeUnit.MINUTES);

    InstanceSyncHandler.InstanceSyncExecutor.registerIterators(injector);
    ApprovalPollingHandler.ApprovalPollingExecutor.registerIterators(injector);
    ScheduleTriggerHandler.registerIterators(injector);
    ECSPollingHandler.ECSPollingExecutor.registerIterators(injector);
    GCPBillingHandler.GCPBillingExecutor.registerIterators(injector);
    SegmentGroupEventJobExecutor.registerIterators(injector);
    BarrierServiceImpl.registerIterators(injector);
    EntityAuditRecordHandler.EntityAuditRecordExecutor.registerIterators(injector);
    if (injector.getInstance(FeatureFlagService.class).isGlobalEnabled(PERPETUAL_TASK_SERVICE)) {
      logger.info("Initializing Perpetual Task Assignor..");
      PerpetualTaskRecordHandler.PerpetualTaskRecordExecutor.registerIterators(injector);
      RecentlyDisconnectedDelegateHandler.RecentlyDisconnectedDelegateExecutor.registerIterators(injector);
    }
  }

  private void registerCronJobs(Injector injector) {
    logger.info("Register cron jobs...");
    final PersistentScheduler jobScheduler =
        injector.getInstance(Key.get(PersistentScheduler.class, Names.named("BackgroundJobScheduler")));

    PersistentLocker persistentLocker = injector.getInstance(Key.get(PersistentLocker.class));

    try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(
             WingsApplication.class, "Initialization", ofSeconds(5), ofSeconds(10))) {
      // If we do not get the lock, that's not critical - that's most likely because other managers took it
      // and they will initialize the jobs.
      if (acquiredLock != null) {
        WorkflowExecutionMonitorJob.add(jobScheduler);
        ResourceConstraintBackupJob.addJob(jobScheduler);
        ZombieHunterJob.scheduleJobs(jobScheduler);
        AdministrativeJob.addJob(jobScheduler);
        LicenseCheckJob.addJob(jobScheduler);
        UsageMetricsJob.addJob(jobScheduler);
        YamlChangeSetPruneJob.add(jobScheduler);
        ExecutionLogsPruneJob.addJob(jobScheduler);
        InstancesPurgeJob.add(jobScheduler);
        AccountIdAdditionJob.addJob(jobScheduler);
      }
    }
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(KryoFeature.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(ConstraintViolationExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);
    environment.jersey().register(GenericExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerAuthFilters(MainConfiguration configuration, Environment environment, Injector injector) {
    if (configuration.isEnableAuth()) {
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
      environment.jersey().register(injector.getInstance(AuthRuleFilter.class));
      environment.jersey().register(injector.getInstance(AuthResponseFilter.class));
      environment.jersey().register(injector.getInstance(AuthenticationFilter.class));
    }
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(LearningEngineService.class).initializeServiceSecretKeys();
  }

  private void runMigrations(Injector injector) {
    injector.getInstance(MigrationService.class).runMigrations();
  }
}
