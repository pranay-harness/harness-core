package software.wings.app;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.inject.matcher.Matchers.not;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_STORE;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static software.wings.beans.FeatureName.GLOBAL_DISABLE_HEALTH_CHECK;
import static software.wings.common.VerificationConstants.CV_24X7_METRIC_LABELS;
import static software.wings.common.VerificationConstants.CV_META_DATA;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;
import static software.wings.common.VerificationConstants.VERIFICATION_METRIC_LABELS;

import com.google.common.collect.ImmutableMap;
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
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
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
import io.harness.NgManagerServiceDriverModule;
import io.harness.artifact.ArtifactCollectionPTaskServiceClient;
import io.harness.cache.CacheModule;
import io.harness.ccm.CEPerpetualTaskHandler;
import io.harness.ccm.KubernetesClusterHandler;
import io.harness.ccm.cluster.ClusterRecordHandler;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.ClusterRecordServiceImpl;
import io.harness.ccm.license.CeLicenseExpiryHandler;
import io.harness.commandlibrary.client.CommandLibraryServiceClientModule;
import io.harness.config.DatadogConfig;
import io.harness.config.PublisherConfiguration;
import io.harness.config.WorkersConfiguration;
import io.harness.configuration.DeployMode;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.perpetualtask.DataCollectionPerpetualTaskServiceClient;
import io.harness.delay.DelayEventListener;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.event.EventsModule;
import io.harness.event.listener.EventListener;
import io.harness.event.reconciliation.service.DeploymentReconExecutorService;
import io.harness.event.reconciliation.service.DeploymentReconTask;
import io.harness.event.usagemetrics.EventsModuleHelper;
import io.harness.exception.WingsException;
import io.harness.execution.export.background.ExportExecutionsRequestCleanupHandler;
import io.harness.execution.export.background.ExportExecutionsRequestHandler;
import io.harness.govern.ProviderModule;
import io.harness.grpc.GrpcServiceConfigurationModule;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.health.HealthMonitor;
import io.harness.health.HealthService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.DistributedLockImplementation;
import io.harness.lock.PersistentLocker;
import io.harness.maintenance.HazelcastListener;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.QuartzCleaner;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.CustomDeploymentInstanceSyncClient;
import io.harness.perpetualtask.ForwardingPerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskServiceImpl;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.AwsSshPerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AzureVMSSInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.internal.DisconnectedDelegateHandler;
import io.harness.perpetualtask.internal.PerpetualTaskRecordHandler;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import io.harness.perpetualtask.remote.RemotePerpetualTaskType;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.scheduler.PersistentScheduler;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.service.DelegateServiceModule;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintPersistenceMonitor;
import io.harness.stream.GuiceObjectFactory;
import io.harness.stream.StreamModule;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import io.harness.workers.background.critical.iterator.ArtifactCollectionHandler;
import io.harness.workers.background.critical.iterator.ResourceConstraintBackupHandler;
import io.harness.workers.background.critical.iterator.WorkflowExecutionMonitorHandler;
import io.harness.workers.background.iterator.ArtifactCleanupHandler;
import io.harness.workers.background.iterator.InstanceSyncHandler;
import io.harness.workers.background.iterator.SettingAttributeValidateConnectivityHandler;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.HttpTransport;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.AdvancedDatastore;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.beans.User;
import software.wings.beans.alert.AlertReconciliationHandler;
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
import software.wings.scheduler.AccountPasswordExpirationJob;
import software.wings.scheduler.InstancesPurgeJob;
import software.wings.scheduler.UsageMetricsHandler;
import software.wings.scheduler.VaultSecretManagerRenewalHandler;
import software.wings.scheduler.YamlChangeSetPruneJob;
import software.wings.scheduler.account.AccountBackgroundJobHandler;
import software.wings.scheduler.account.LicenseCheckHandler;
import software.wings.scheduler.approval.ApprovalPollingHandler;
import software.wings.scheduler.audit.EntityAuditRecordHandler;
import software.wings.scheduler.events.segment.SegmentGroupEventJob;
import software.wings.scheduler.marketplace.gcp.GCPBillingHandler;
import software.wings.scheduler.persistance.PersistentLockCleanup;
import software.wings.search.framework.ElasticsearchSyncService;
import software.wings.security.AuthResponseFilter;
import software.wings.security.AuthRuleFilter;
import software.wings.security.AuthenticationFilter;
import software.wings.security.LoginRateLimitFilter;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.security.encryption.migration.EncryptedDataAwsToGcpKmsMigrationHandler;
import software.wings.security.encryption.migration.SettingAttributesSecretReferenceFeatureFlagJob;
import software.wings.security.encryption.migration.SettingAttributesSecretsMigrationHandler;
import software.wings.service.impl.AccountServiceImpl;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.BarrierServiceImpl;
import software.wings.service.impl.DelegateProfileServiceImpl;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.ExecutionEventListener;
import software.wings.service.impl.InfrastructureMappingServiceImpl;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.artifact.ArtifactStreamPTaskManager;
import software.wings.service.impl.artifact.ArtifactStreamPTaskMigrationJob;
import software.wings.service.impl.artifact.ArtifactStreamSettingAttributePTaskManager;
import software.wings.service.impl.event.DeploymentTimeSeriesEventListener;
import software.wings.service.impl.infrastructuredefinition.InfrastructureDefinitionServiceImpl;
import software.wings.service.impl.instance.DeploymentEventListener;
import software.wings.service.impl.instance.InstanceEventListener;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskMigrationJob;
import software.wings.service.impl.security.KmsTransitionEventListener;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.impl.yaml.YamlPushServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateMachineExecutor;
import software.wings.yaml.gitSync.GitChangeSetRunnable;
import software.wings.yaml.gitSync.GitSyncEntitiesExpiryHandler;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
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
  private static final SecureRandom random = new SecureRandom();

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
    bootstrap.addCommand(new InspectCommand<>(this));

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

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 1000, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(ManagerRegistrars.kryoRegistrars).build();
      }
      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(ManagerRegistrars.morphiaRegistrars)
            .build();
      }
    });

    modules.add(new WingsPersistenceModule());

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration.getCacheConfig());
    modules.addAll(cacheModule.cumulativeDependencies());
    StreamModule streamModule = new StreamModule(configuration.getAtmosphereBroadcaster());
    modules.addAll(streamModule.cumulativeDependencies());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
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
    modules.add(new DelegateServiceModule());
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .build();
      }
    });

    modules.add(new IndexMigratorModule());
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
    modules.add(new GrpcServiceConfigurationModule(
        configuration.getGrpcServerConfig(), configuration.getPortal().getJwtNextGenManagerSecret()));

    modules.add(new NgManagerServiceDriverModule(
        configuration.getGrpcClientConfig(), "manager", configuration.getPortal().getJwtNextGenManagerSecret()));

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      WorkersConfiguration workersConfig() {
        return configuration.getWorkers();
      }

      @Provides
      @Singleton
      PublisherConfiguration publisherConfiguration() {
        return configuration.getPublisherConfiguration();
      }
    });

    modules.add(new CommandLibraryServiceClientModule(configuration.getCommandLibraryServiceConfig()));

    Injector injector = Guice.createInjector(modules);

    // Access all caches before coming out of maintenance
    injector.getInstance(new Key<Map<String, Cache<?, ?>>>() {});

    registerAtmosphereStreams(environment, injector);

    initializeFeatureFlags(injector);

    registerHealthChecks(environment, injector);

    registerStores(configuration, injector);

    registerResources(environment, injector);

    registerManagedBeans(configuration, environment, injector);

    registerQueueListeners(injector);

    scheduleJobs(injector, configuration);

    registerObservers(injector);

    registerInprocPerpetualTaskServiceClients(injector);
    registerRemotePerpetualTaskServiceClients(injector);

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
    logger.info("Initializing gRPC server...");
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    registerDatadogPublisherIfEnabled(configuration);

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");
    logger.info("Manager is running on JRE: {}", System.getProperty("java.version"));
  }

  private void registerAtmosphereStreams(Environment environment, Injector injector) {
    injector.getInstance(BroadcasterFactory.class);
    injector.getInstance(MetaBroadcaster.class);
    AtmosphereServlet atmosphereServlet = injector.getInstance(AtmosphereServlet.class);
    atmosphereServlet.framework().objectFactory(new GuiceObjectFactory(injector));
    ServletRegistration.Dynamic dynamic = environment.servlets().addServlet("StreamServlet", atmosphereServlet);
    dynamic.setAsyncSupported(true);
    dynamic.setLoadOnStartup(0);
    dynamic.addMapping("/stream/*");
  }

  private void registerInprocPerpetualTaskServiceClients(Injector injector) {
    PerpetualTaskServiceClientRegistry clientRegistry =
        injector.getInstance(Key.get(PerpetualTaskServiceClientRegistry.class));

    clientRegistry.registerClient(
        PerpetualTaskType.K8S_WATCH, injector.getInstance(K8sWatchPerpetualTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.ECS_CLUSTER, injector.getInstance(EcsPerpetualTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.SAMPLE, injector.getInstance(SamplePerpetualTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.ARTIFACT_COLLECTION, injector.getInstance(ArtifactCollectionPTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.PCF_INSTANCE_SYNC, injector.getInstance(PcfInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.AWS_SSH_INSTANCE_SYNC, injector.getInstance(AwsSshPerpetualTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.AWS_AMI_INSTANCE_SYNC, injector.getInstance(AwsAmiInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.AWS_CODE_DEPLOY_INSTANCE_SYNC,
        injector.getInstance(AwsCodeDeployInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.SPOT_INST_AMI_INSTANCE_SYNC,
        injector.getInstance(SpotinstAmiInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.AZURE_VMSS_INSTANCE_SYNC,
        injector.getInstance(AzureVMSSInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.CONTAINER_INSTANCE_SYNC,
        injector.getInstance(ContainerInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC,
        injector.getInstance(AwsLambdaInstanceSyncPerpetualTaskClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.DATA_COLLECTION_TASK, injector.getInstance(DataCollectionPerpetualTaskServiceClient.class));
    clientRegistry.registerClient(PerpetualTaskType.CUSTOM_DEPLOYMENT_INSTANCE_SYNC,
        injector.getInstance(CustomDeploymentInstanceSyncClient.class));
  }

  private void registerRemotePerpetualTaskServiceClients(Injector injector) {
    final PerpetualTaskServiceClientRegistry clientRegistry =
        injector.getInstance(Key.get(PerpetualTaskServiceClientRegistry.class));

    asList(RemotePerpetualTaskType.values()).forEach(remotePerpetualTaskType -> {
      String taskType = remotePerpetualTaskType.getTaskType();
      String serviceId = remotePerpetualTaskType.getOwnerServiceId();
      final ForwardingPerpetualTaskServiceClient instance =
          new ForwardingPerpetualTaskServiceClient(taskType, serviceId);
      injector.injectMembers(instance);
      clientRegistry.registerClient(taskType, instance);
    });
  }

  private void registerDatadogPublisherIfEnabled(MainConfiguration configuration) {
    DatadogConfig datadogConfig = configuration.getDatadogConfig();
    if (datadogConfig != null && datadogConfig.isEnabled()) {
      try {
        logger.info("Registering datadog javaagent");
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(datadogConfig.getApiKey()).build();
        DatadogReporter reporter = DatadogReporter.forRegistry(harnessMetricRegistry.getThreadPoolMetricRegistry())
                                       .withTransport(httpTransport)
                                       .build();

        reporter.start(60, TimeUnit.SECONDS);
        logger.info("Registered datadog javaagent");
      } catch (Exception t) {
        logger.error("Error while initializing datadog", t);
      }
    }
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
      healthService.registerMonitor((HealthMonitor) injector.getInstance(PersistentLocker.class));
    }
  }

  private void registerStores(MainConfiguration configuration, Injector injector) {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    if (configuration.getDistributedLockImplementation() == DistributedLockImplementation.MONGO
        && isNotEmpty(configuration.getMongoConnectionFactory().getLocksUri())
        && !configuration.getMongoConnectionFactory().getLocksUri().equals(
               configuration.getMongoConnectionFactory().getUri())) {
      persistence.register(LOCKS_STORE, configuration.getMongoConnectionFactory().getLocksUri());
    }
    if (isNotEmpty(configuration.getEventsMongo().getUri())
        && !configuration.getEventsMongo().getUri().equals(configuration.getMongoConnectionFactory().getUri())) {
      persistence.register(Store.builder().name("events").build(), configuration.getEventsMongo().getUri());
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
    environment.lifecycle().manage((Managed) injector.getInstance(PersistentLocker.class));
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
    environment.lifecycle().manage(injector.getInstance(ConfigurationController.class));
    environment.lifecycle().manage(injector.getInstance(TimerScheduledExecutorService.class));
    environment.lifecycle().manage(injector.getInstance(NotifierScheduledExecutorService.class));
    environment.lifecycle().manage((Managed) injector.getInstance(ExecutorService.class));
    environment.lifecycle().manage(injector.getInstance(SettingAttributesSecretReferenceFeatureFlagJob.class));
    environment.lifecycle().manage(injector.getInstance(ArtifactStreamPTaskMigrationJob.class));
    environment.lifecycle().manage(injector.getInstance(InstanceSyncPerpetualTaskMigrationJob.class));
    if (configuration.isSearchEnabled()) {
      environment.lifecycle().manage(injector.getInstance(ElasticsearchSyncService.class));
    }
  }

  private void registerWaitEnginePublishers(Injector injector) {
    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(GENERAL, payload -> publisher.send(asList(GENERAL), payload));
    notifyQueuePublisherRegister.register(ORCHESTRATION, payload -> publisher.send(asList(ORCHESTRATION), payload));
  }

  private void registerQueueListeners(Injector injector) {
    logger.info("Initializing queue listeners...");

    registerWaitEnginePublishers(injector);

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
    queueListenerController.register(injector.getInstance(GeneralNotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(OrchestrationNotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(PruneEntityListener.class), 1);
  }

  private void scheduleJobs(Injector injector, MainConfiguration configuration) {
    logger.info("Initializing scheduled jobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), random.nextInt(300), 300L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("delegateTaskNotifier")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateQueueTask.class), random.nextInt(5), 5L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("gitChangeSet")))
        .scheduleWithFixedDelay(
            injector.getInstance(GitChangeSetRunnable.class), random.nextInt(4), 4L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateSyncServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
    if (configuration.getDistributedLockImplementation() == DistributedLockImplementation.MONGO) {
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
          .scheduleWithFixedDelay(
              injector.getInstance(PersistentLockCleanup.class), random.nextInt(60), 60L, TimeUnit.MINUTES);
    }
    injector.getInstance(DeploymentReconExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(DeploymentReconTask.class), random.nextInt(60), 15 * 60L, TimeUnit.SECONDS);

    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(()
                                    -> injector.getInstance(PerpetualTaskServiceImpl.class).broadcastToDelegate(),
            0L, 10L, TimeUnit.SECONDS);
  }

  public static void registerObservers(Injector injector) {
    // Register Audit observer
    YamlPushServiceImpl yamlPushService = (YamlPushServiceImpl) injector.getInstance(Key.get(YamlPushService.class));
    AuditServiceImpl auditService = (AuditServiceImpl) injector.getInstance(Key.get(AuditService.class));
    yamlPushService.getEntityCrudSubject().register(auditService);

    AuditServiceHelper auditServiceHelper = injector.getInstance(Key.get(AuditServiceHelper.class));
    auditServiceHelper.getEntityCrudSubject().register(auditService);

    ClusterRecordHandler clusterRecordHandler = injector.getInstance(Key.get(ClusterRecordHandler.class));
    SettingsServiceImpl settingsService = (SettingsServiceImpl) injector.getInstance(Key.get(SettingsService.class));
    settingsService.getSubject().register(clusterRecordHandler);
    settingsService.getArtifactStreamSubject().register(
        injector.getInstance(Key.get(ArtifactStreamSettingAttributePTaskManager.class)));

    KubernetesClusterHandler kubernetesClusterHandler = injector.getInstance(Key.get(KubernetesClusterHandler.class));
    DelegateServiceImpl delegateService = (DelegateServiceImpl) injector.getInstance(Key.get(DelegateService.class));
    delegateService.getSubject().register(kubernetesClusterHandler);

    InfrastructureDefinitionServiceImpl infrastructureDefinitionService =
        (InfrastructureDefinitionServiceImpl) injector.getInstance(Key.get(InfrastructureDefinitionService.class));
    infrastructureDefinitionService.getSubject().register(clusterRecordHandler);

    InfrastructureMappingServiceImpl infrastructureMappingService =
        (InfrastructureMappingServiceImpl) injector.getInstance(Key.get(InfrastructureMappingService.class));
    infrastructureMappingService.getSubject().register(clusterRecordHandler);

    CEPerpetualTaskHandler cePerpetualTaskHandler = injector.getInstance(Key.get(CEPerpetualTaskHandler.class));
    ClusterRecordServiceImpl clusterRecordService =
        (ClusterRecordServiceImpl) injector.getInstance(Key.get(ClusterRecordService.class));
    clusterRecordService.getSubject().register(cePerpetualTaskHandler);

    ArtifactStreamServiceImpl artifactStreamService =
        (ArtifactStreamServiceImpl) injector.getInstance(Key.get(ArtifactStreamService.class));
    artifactStreamService.getSubject().register(injector.getInstance(Key.get(ArtifactStreamPTaskManager.class)));

    AccountServiceImpl accountService = (AccountServiceImpl) injector.getInstance(Key.get(AccountService.class));
    accountService.getAccountCrudSubject().register(
        (DelegateProfileServiceImpl) injector.getInstance(Key.get(DelegateProfileService.class)));
    accountService.getAccountCrudSubject().register(injector.getInstance(Key.get(CEPerpetualTaskHandler.class)));

    PerpetualTaskServiceImpl perpetualTaskService =
        (PerpetualTaskServiceImpl) injector.getInstance(Key.get(PerpetualTaskService.class));
    perpetualTaskService.getPerpetualTaskCrudSubject().register(
        injector.getInstance(Key.get(PerpetualTaskRecordHandler.class)));

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

    injector.getInstance(AlertReconciliationHandler.class).registerIterators();
    injector.getInstance(ArtifactCollectionHandler.class).registerIterators(artifactCollectionExecutor);
    injector.getInstance(ArtifactCleanupHandler.class).registerIterators(artifactCollectionExecutor);
    injector.getInstance(InstanceSyncHandler.class).registerIterators();
    injector.getInstance(LicenseCheckHandler.class).registerIterators();
    injector.getInstance(ApprovalPollingHandler.class).registerIterators();
    injector.getInstance(GCPBillingHandler.class).registerIterators();
    injector.getInstance(SegmentGroupEventJob.class).registerIterators();
    injector.getInstance(BarrierServiceImpl.class).registerIterators();
    injector.getInstance(EntityAuditRecordHandler.class).registerIterators();
    injector.getInstance(UsageMetricsHandler.class).registerIterators();
    injector.getInstance(ResourceConstraintBackupHandler.class).registerIterators();
    injector.getInstance(WorkflowExecutionMonitorHandler.class).registerIterators();
    injector.getInstance(SettingAttributeValidateConnectivityHandler.class).registerIterators();
    injector.getInstance(PerpetualTaskRecordHandler.class).registerIterators();
    injector.getInstance(DisconnectedDelegateHandler.class).registerIterators();
    injector.getInstance(VaultSecretManagerRenewalHandler.class).registerIterators();
    injector.getInstance(EncryptedDataAwsToGcpKmsMigrationHandler.class).registerIterators();
    injector.getInstance(SettingAttributesSecretsMigrationHandler.class).registerIterators();
    injector.getInstance(GitSyncEntitiesExpiryHandler.class).registerIterators();
    injector.getInstance(ExportExecutionsRequestHandler.class).registerIterators();
    injector.getInstance(ExportExecutionsRequestCleanupHandler.class).registerIterators();
    injector.getInstance(AccountBackgroundJobHandler.class).registerIterators();
    injector.getInstance(io.harness.steps.barriers.service.BarrierServiceImpl.class).registerIterators();
    injector.getInstance(CeLicenseExpiryHandler.class).registerIterators();
    injector.getInstance(ResourceRestraintPersistenceMonitor.class).registerIterators();
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
        YamlChangeSetPruneJob.add(jobScheduler);
        InstancesPurgeJob.add(jobScheduler);
        AccountPasswordExpirationJob.add(jobScheduler);
      }
    }

    WingsPersistence wingsPersistence = injector.getInstance(Key.get(WingsPersistence.class));

    new Thread(() -> {
      AdvancedDatastore datastore = wingsPersistence.getDatastore(HPersistence.DEFAULT_STORE);
      QuartzCleaner.cleanup(datastore, "quartz");
      QuartzCleaner.cleanup(datastore, "quartz_verification");
    })
        .start();
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
      environment.jersey().register(injector.getInstance(LoginRateLimitFilter.class));
      environment.jersey().register(injector.getInstance(AuthRuleFilter.class));
      environment.jersey().register(injector.getInstance(AuthResponseFilter.class));
      environment.jersey().register(injector.getInstance(AuthenticationFilter.class));
    }
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(VerificationServiceSecretManager.class).initializeServiceSecretKeys();
  }

  private void runMigrations(Injector injector) {
    injector.getInstance(MigrationService.class).runMigrations();
  }
}
