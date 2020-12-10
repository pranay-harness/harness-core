package software.wings.app;

import static io.harness.beans.FeatureName.GLOBAL_DISABLE_HEALTH_CHECK;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.lock.mongo.MongoPersistentLocker.LOCKS_STORE;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.time.DurationUtils.durationTillDayTime;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import static software.wings.common.VerificationConstants.CV_24X7_METRIC_LABELS;
import static software.wings.common.VerificationConstants.CV_META_DATA;
import static software.wings.common.VerificationConstants.VERIFICATION_DEPLOYMENTS;
import static software.wings.common.VerificationConstants.VERIFICATION_METRIC_LABELS;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.inject.matcher.Matchers.not;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;

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
import io.harness.cvng.client.CVNGClientModule;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.state.CVNGVerificationTaskHandler;
import io.harness.dataretention.AccountDataRetentionEntity;
import io.harness.delay.DelayEventListener;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.engine.events.OrchestrationEventListener;
import io.harness.engine.pms.sdk.NodeExecutionEventListener;
import io.harness.event.EventsModule;
import io.harness.event.listener.EventListener;
import io.harness.event.reconciliation.service.DeploymentReconExecutorService;
import io.harness.event.reconciliation.service.DeploymentReconTask;
import io.harness.event.usagemetrics.EventsModuleHelper;
import io.harness.exception.WingsException;
import io.harness.execution.export.background.ExportExecutionsRequestCleanupHandler;
import io.harness.execution.export.background.ExportExecutionsRequestHandler;
import io.harness.ff.FeatureFlagService;
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
import io.harness.manifest.ManifestCollectionPTaskServiceClient;
import io.harness.marketplace.gcp.GcpMarketplaceSubscriberService;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoModule;
import io.harness.mongo.QuartzCleaner;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.CorrelationFilter;
import io.harness.perpetualtask.AwsAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.AwsCodeDeployInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.CustomDeploymentInstanceSyncClient;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskServiceImpl;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.connector.ConnectorHeartbeatPerpetualTaskClient;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.AwsSshPerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AzureVMSSInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.AzureWebAppInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.ContainerInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.internal.PerpetualTaskRecordHandler;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.steps.StepType;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.scheduler.PersistentScheduler;
import io.harness.secrets.SecretMigrationEventListener;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.json.FailureInfoSerializer;
import io.harness.serializer.json.StepTypeSerializer;
import io.harness.service.DelegateServiceModule;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintPersistenceMonitor;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.stream.GuiceObjectFactory;
import io.harness.stream.StreamModule;
import io.harness.threading.ExecutorModule;
import io.harness.threading.Schedulable;
import io.harness.threading.ThreadPool;
import io.harness.timeout.TimeoutEngine;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import io.harness.waiter.ProgressUpdateService;
import io.harness.workers.background.critical.iterator.ArtifactCollectionHandler;
import io.harness.workers.background.critical.iterator.ResourceConstraintBackupHandler;
import io.harness.workers.background.critical.iterator.WorkflowExecutionMonitorHandler;
import io.harness.workers.background.iterator.ArtifactCleanupHandler;
import io.harness.workers.background.iterator.InstanceSyncHandler;
import io.harness.workers.background.iterator.SettingAttributeValidateConnectivityHandler;

import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
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
import software.wings.scheduler.DeletedEntityHandler;
import software.wings.scheduler.InstancesPurgeJob;
import software.wings.scheduler.UsageMetricsHandler;
import software.wings.scheduler.VaultSecretManagerRenewalHandler;
import software.wings.scheduler.YamlChangeSetPruneJob;
import software.wings.scheduler.account.DeleteAccountHandler;
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
import software.wings.security.encryption.migration.SettingAttributesSecretReferenceFeatureFlagJob;
import software.wings.security.encryption.migration.SettingAttributesSecretsMigrationHandler;
import software.wings.service.impl.AccountServiceImpl;
import software.wings.service.impl.ApplicationManifestServiceImpl;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.BarrierServiceImpl;
import software.wings.service.impl.DelegateProfileServiceImpl;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.ExecutionEventListener;
import software.wings.service.impl.InfrastructureMappingServiceImpl;
import software.wings.service.impl.PollingModeDelegateDisconnectedDetector;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.applicationmanifest.ManifestPerpetualTaskManger;
import software.wings.service.impl.artifact.ArtifactStreamPTaskManager;
import software.wings.service.impl.artifact.ArtifactStreamPTaskMigrationJob;
import software.wings.service.impl.artifact.ArtifactStreamSettingAttributePTaskManager;
import software.wings.service.impl.event.DeploymentTimeSeriesEventListener;
import software.wings.service.impl.infrastructuredefinition.InfrastructureDefinitionServiceImpl;
import software.wings.service.impl.instance.DeploymentEventListener;
import software.wings.service.impl.instance.InstanceEventListener;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskMigrationJob;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.impl.yaml.YamlPushServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineExecutor;
import software.wings.yaml.gitSync.GitChangeSetRunnable;
import software.wings.yaml.gitSync.GitSyncEntitiesExpiryHandler;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.google.common.collect.ImmutableList;
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
import org.mongodb.morphia.converters.TypeConverter;
import org.reflections.Reflections;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

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
      log.info("Shutdown hook, entering maintenance...");
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
    log.info("bootstrapping ...");
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

    log.info("bootstrapping done.");
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    SimpleModule module = new SimpleModule();
    // Todo: Discuss with Prashant on having an impl just like Morphia Converters
    module.addSerializer(StepType.class, new StepTypeSerializer());
    module.addSerializer(FailureInfo.class, new FailureInfoSerializer());
    mapper.registerModule(module);
    mapper.addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    final AnnotationAwareJsonSubtypeResolver subtypeResolver =
        AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver());
    mapper.setSubtypeResolver(subtypeResolver);
    mapper.setConfig(mapper.getSerializationConfig().withView(JsonViews.Public.class));
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
      // defining a different serialVersionUID then base. We don't care about serializing it.
      private static final long serialVersionUID = 7777451630128399020L;
      @Override
      public List<NamedType> findSubtypes(Annotated a) {
        final List<NamedType> subtypesFromSuper = super.findSubtypes(a);
        if (isNotEmpty(subtypesFromSuper)) {
          return subtypesFromSuper;
        }
        return emptyIfNull(subtypeResolver.findSubtypes(a));
      }
    });
  }

  @Override
  public void run(final MainConfiguration configuration, Environment environment) throws Exception {
    log.info("Starting app ...");
    log.info("Entering startup maintenance mode");
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

    modules.add(MongoModule.getInstance());
    modules.add(new SpringPersistenceModule());

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration.getCacheConfig());
    modules.add(cacheModule);
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AtmosphereBroadcaster atmosphereBroadcaster() {
        return configuration.getAtmosphereBroadcaster();
      }
    });
    modules.add(StreamModule.getInstance());

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
    modules.add(new WingsModule(configuration));
    modules.add(new CVNGClientModule(configuration.getCvngClientConfig()));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .put(DelegateTaskProgressResponse.class, "delegateTaskProgressResponses")
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

    initializeFeatureFlags(configuration, injector);

    registerHealthChecks(environment, injector);

    registerStores(configuration, injector);

    registerResources(environment, injector);

    registerManagedBeans(configuration, environment, injector);

    registerQueueListeners(injector);

    scheduleJobs(injector, configuration);

    registerObservers(injector);

    registerInprocPerpetualTaskServiceClients(injector);

    registerCronJobs(injector);

    registerCorsFilter(configuration, environment);

    registerAuditResponseFilter(environment, injector);

    registerJerseyProviders(environment, injector);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(configuration, environment, injector);

    registerCorrelationFilter(environment, injector);

    // Register collection iterators
    if (configuration.isEnableIterators()) {
      registerIterators(injector);
    }
    registerCVNGVerificationTaskIterator(injector);

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

    initializeServiceSecretKeys(injector);

    runMigrations(injector);

    String deployMode = configuration.getDeployMode().name();

    if (DeployMode.isOnPrem(deployMode)) {
      LicenseService licenseService = injector.getInstance(LicenseService.class);
      String encryptedLicenseInfoBase64String = System.getenv(LicenseService.LICENSE_INFO);
      log.info("Encrypted license info read from environment {}", encryptedLicenseInfoBase64String);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        log.error("No license info is provided");
      } else {
        try {
          log.info("Updating license info read from environment {}", encryptedLicenseInfoBase64String);
          licenseService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
          log.info("Updated license info read from environment {}", encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          log.error("Error while updating license info", ex);
        }
      }
    }

    injector.getInstance(EventsModuleHelper.class).initialize();
    log.info("Initializing gRPC server...");
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    registerDatadogPublisherIfEnabled(configuration);

    log.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    log.info("Starting app done");
    log.info("Manager is running on JRE: {}", System.getProperty("java.version"));
  }

  private void registerCVNGVerificationTaskIterator(Injector injector) {
    injector.getInstance(CVNGVerificationTaskHandler.class).registerIterator();
  }

  private void registerAtmosphereStreams(Environment environment, Injector injector) {
    AtmosphereServlet atmosphereServlet = injector.getInstance(AtmosphereServlet.class);
    atmosphereServlet.framework().objectFactory(new GuiceObjectFactory(injector));
    injector.getInstance(BroadcasterFactory.class);
    injector.getInstance(MetaBroadcaster.class);
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
    clientRegistry.registerClient(PerpetualTaskType.CUSTOM_DEPLOYMENT_INSTANCE_SYNC,
        injector.getInstance(CustomDeploymentInstanceSyncClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.MANIFEST_COLLECTION, injector.getInstance(ManifestCollectionPTaskServiceClient.class));
    clientRegistry.registerClient(
        PerpetualTaskType.CONNECTOR_TEST_CONNECTION, injector.getInstance(ConnectorHeartbeatPerpetualTaskClient.class));
    clientRegistry.registerClient(PerpetualTaskType.AZURE_WEB_APP_INSTANCE_SYNC,
        injector.getInstance(AzureWebAppInstanceSyncPerpetualTaskClient.class));
  }

  private void registerDatadogPublisherIfEnabled(MainConfiguration configuration) {
    DatadogConfig datadogConfig = configuration.getDatadogConfig();
    if (datadogConfig != null && datadogConfig.isEnabled()) {
      try {
        log.info("Registering datadog javaagent");
        HttpTransport httpTransport = new HttpTransport.Builder().withApiKey(datadogConfig.getApiKey()).build();
        DatadogReporter reporter = DatadogReporter.forRegistry(harnessMetricRegistry.getThreadPoolMetricRegistry())
                                       .withTransport(httpTransport)
                                       .build();

        reporter.start(60, TimeUnit.SECONDS);
        log.info("Registered datadog javaagent");
      } catch (Exception t) {
        log.error("Error while initializing datadog", t);
      }
    }
  }

  private void initMetrics() {
    harnessMetricRegistry.registerCounterMetric(
        VERIFICATION_DEPLOYMENTS, VERIFICATION_METRIC_LABELS.toArray(new String[0]), " ");
    harnessMetricRegistry.registerGaugeMetric(CV_META_DATA, CV_24X7_METRIC_LABELS, " ");
  }

  private void initializeFeatureFlags(MainConfiguration mainConfiguration, Injector injector) {
    injector.getInstance(FeatureFlagService.class)
        .initializeFeatureFlags(mainConfiguration.getDeployMode(), mainConfiguration.getFeatureNames());
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
    environment.lifecycle().manage(injector.getInstance(GcpMarketplaceSubscriberService.class));
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

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }

  private void registerQueueListeners(Injector injector) {
    log.info("Initializing queue listeners...");

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
    queueListenerController.register(injector.getInstance(SecretMigrationEventListener.class), 1);
    queueListenerController.register(injector.getInstance(GeneralNotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(OrchestrationNotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(PruneEntityListener.class), 1);
    queueListenerController.register(injector.getInstance(OrchestrationEventListener.class), 1);
    queueListenerController.register(injector.getInstance(NodeExecutionEventListener.class), 1);
  }

  private void scheduleJobs(Injector injector, MainConfiguration configuration) {
    log.info("Initializing scheduled jobs...");
    ScheduledExecutorService taskPollExecutor =
        injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")));

    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), random.nextInt(300), 300L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("delegateTaskNotifier")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateQueueTask.class), random.nextInt(5), 5L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("gitChangeSet")))
        .scheduleWithFixedDelay(
            injector.getInstance(GitChangeSetRunnable.class), random.nextInt(4), 4L, TimeUnit.SECONDS);
    taskPollExecutor.scheduleWithFixedDelay(
        injector.getInstance(DelegateSyncServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
    if (configuration.getDistributedLockImplementation() == DistributedLockImplementation.MONGO) {
      taskPollExecutor.scheduleWithFixedDelay(
          injector.getInstance(PersistentLockCleanup.class), random.nextInt(60), 60L, TimeUnit.MINUTES);
    }
    injector.getInstance(DeploymentReconExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(DeploymentReconTask.class), random.nextInt(60), 15 * 60L, TimeUnit.SECONDS);

    taskPollExecutor.scheduleWithFixedDelay(
        () -> injector.getInstance(PerpetualTaskServiceImpl.class).broadcastToDelegate(), 0L, 10L, TimeUnit.SECONDS);

    taskPollExecutor.scheduleWithFixedDelay(
        new Schedulable("Failed updating delegate connection disconnected flag to true",
            injector.getInstance(PollingModeDelegateDisconnectedDetector.class)),
        0L, 60L, TimeUnit.SECONDS);

    taskPollExecutor.scheduleWithFixedDelay(
        injector.getInstance(ProgressUpdateService.class), 0L, 5L, TimeUnit.SECONDS);

    ImmutableList<Class<? extends AccountDataRetentionEntity>> classes =
        ImmutableList.<Class<? extends AccountDataRetentionEntity>>builder()
            .add(WorkflowExecution.class)
            .add(StateExecutionInstance.class)
            .add(Activity.class)
            .add(Log.class)
            .build();

    taskPollExecutor.scheduleWithFixedDelay(
        new Schedulable("Failed ensure data retention",
            () -> { injector.getInstance(AccountService.class).ensureDataRetention(classes); }),
        durationTillDayTime(System.currentTimeMillis(), ofHours(10)).toMillis(), ofHours(24).toMillis(),
        TimeUnit.MILLISECONDS);

    taskPollExecutor.scheduleAtFixedRate(new Schedulable("Failed ensure data retention", () -> {
      Map<String, Long> dataRetentionMap = injector.getInstance(AccountService.class).obtainAccountDataRetentionMap();
      injector.getInstance(DataStoreService.class).purgeDataRetentionOlderRecords(dataRetentionMap);
    }), 0, 60L, TimeUnit.MINUTES);
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

    DelegateServiceImpl delegateServiceImpl =
        (DelegateServiceImpl) injector.getInstance(Key.get(DelegateService.class));
    delegateServiceImpl.getSubject().register(perpetualTaskService);

    PollingModeDelegateDisconnectedDetector pollingModeDelegateDisconnectedDetector =
        injector.getInstance(Key.get(PollingModeDelegateDisconnectedDetector.class));
    pollingModeDelegateDisconnectedDetector.getSubject().register(perpetualTaskService);

    ApplicationManifestServiceImpl applicationManifestService =
        (ApplicationManifestServiceImpl) injector.getInstance(Key.get(ApplicationManifestService.class));
    applicationManifestService.getSubject().register(injector.getInstance(Key.get(ManifestPerpetualTaskManger.class)));

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
    injector.getInstance(VaultSecretManagerRenewalHandler.class).registerIterators();
    injector.getInstance(SettingAttributesSecretsMigrationHandler.class).registerIterators();
    injector.getInstance(GitSyncEntitiesExpiryHandler.class).registerIterators();
    injector.getInstance(ExportExecutionsRequestHandler.class).registerIterators();
    injector.getInstance(ExportExecutionsRequestCleanupHandler.class).registerIterators();
    injector.getInstance(io.harness.steps.barriers.service.BarrierServiceImpl.class).registerIterators();
    injector.getInstance(CeLicenseExpiryHandler.class).registerIterators();
    injector.getInstance(ResourceRestraintPersistenceMonitor.class).registerIterators();
    injector.getInstance(DeleteAccountHandler.class).registerIterators();
    injector.getInstance(TimeoutEngine.class).registerIterators();
    injector.getInstance(DeletedEntityHandler.class).registerIterators();
  }

  private void registerCronJobs(Injector injector) {
    log.info("Register cron jobs...");
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
    }).start();
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(KryoFeature.class));
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
