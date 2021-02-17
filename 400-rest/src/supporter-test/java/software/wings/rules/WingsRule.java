package software.wings.rules;

import static io.harness.cache.CacheBackend.CAFFEINE;
import static io.harness.cache.CacheBackend.NOOP;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.maintenance.MaintenanceController.forceMaintenance;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;

import io.harness.NoopStatement;
import io.harness.cache.CacheConfig;
import io.harness.cache.CacheConfig.CacheConfigBuilder;
import io.harness.cache.CacheModule;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.config.PublisherConfiguration;
import io.harness.event.EventsModule;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.client.ManagerGrpcClientModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.redis.RedisConfig;
import io.harness.registrars.OrchestrationModuleRegistrarHelper;
import io.harness.registrars.OrchestrationStepsModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationStepsModuleFacilitatorRegistrar;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;
import io.harness.registrars.WingsAdviserRegistrar;
import io.harness.registrars.WingsStepRegistrar;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.Cache;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.kryo.TestManagerKryoRegistrar;
import io.harness.serializer.morphia.ManagerTestMorphiaRegistrar;
import io.harness.service.DelegateServiceModule;
import io.harness.springdata.SpringPersistenceTestModule;
import io.harness.testlib.RealMongo;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;

import software.wings.WingsTestModule;
import software.wings.app.AuthModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.app.IndexMigratorModule;
import software.wings.app.JobsFrequencyConfig;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.ObserversHelper;
import software.wings.app.SSOModule;
import software.wings.app.SignupModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.integration.IntegrationTestBase;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.service.impl.EventEmitter;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
public class WingsRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  protected ClosingFactory closingFactory = new ClosingFactory();

  protected Configuration configuration;
  protected Injector injector;
  private int port;
  private ExecutorService executorService = new CurrentThreadExecutor();

  private static final String JWT_PASSWORD_SECRET = "123456789";

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    if (isIntegrationTest(target)) {
      return new NoopStatement();
    }
    Statement wingsStatement = new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try (GlobalContextGuard ignore = GlobalContextManager.ensureGlobalContextGuard()) {
          upsertGlobalContextRecord(AuditGlobalContextData.builder().auditId("testing").build());
          List<Annotation> annotations = Lists.newArrayList(asList(frameworkMethod.getAnnotations()));
          annotations.addAll(asList(target.getClass().getAnnotations()));
          before(annotations, isIntegrationTest(target),
              target.getClass().getSimpleName() + "." + frameworkMethod.getName());
          injector.injectMembers(target);
          try {
            statement.evaluate();
          } finally {
            after(annotations);
          }
        }
      }
    };

    return wingsStatement;
  }

  protected boolean isIntegrationTest(Object target) {
    return target instanceof IntegrationTestBase;
  }

  /**
   * Before.
   *
   * @param annotations                   the annotations
   * @param doesExtendBaseIntegrationTest the does extend base integration test
   * @param testName                      the test name  @throws Throwable the throwable
   * @throws Throwable the throwable
   */
  protected void before(List<Annotation> annotations, boolean doesExtendBaseIntegrationTest, String testName)
      throws Throwable {
    setProperty("javax.cache.spi.CachingProvider", "com.hazelcast.cache.HazelcastCachingProvider");
    initializeLogging();
    forceMaintenance(false);
    MongoClient mongoClient;
    String dbName = getProperty("dbName", "harness");

    configuration = getConfiguration(annotations, dbName);

    List<Module> modules = modules(annotations);
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return getKryoRegistrars();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return getMorphiaRegistrars();
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
    addQueueModules(modules);

    CacheConfigBuilder cacheConfigBuilder =
        CacheConfig.builder().disabledCaches(new HashSet<>()).cacheNamespace("harness-cache");
    if (annotations.stream().anyMatch(annotation -> annotation instanceof Cache)) {
      cacheConfigBuilder.cacheBackend(CAFFEINE);
    } else {
      cacheConfigBuilder.cacheBackend(NOOP);
    }
    CacheModule cacheModule = new CacheModule(cacheConfigBuilder.build());
    modules.add(0, cacheModule);
    addPMSSdkModule(modules);
    long start = currentTimeMillis();
    injector = Guice.createInjector(modules);
    long diff = currentTimeMillis() - start;
    log.info("Creating guice injector took: {}ms", diff);
    registerListeners(annotations.stream().filter(Listeners.class ::isInstance).findFirst());
    registerScheduledJobs(injector);
    registerProviders();
    registerObservers();

    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }
  }

  public void addPMSSdkModule(List<Module> modules) {
    modules.add(PmsSdkModule.getInstance(getPmsSdkConfiguration()));
  }

  private PmsSdkConfiguration getPmsSdkConfiguration() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers());
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers());
    return PmsSdkConfiguration.builder()
        .deploymentMode(DeployMode.LOCAL)
        .serviceName("wings")
        .engineSteps(WingsStepRegistrar.getEngineSteps())
        .engineAdvisers(WingsAdviserRegistrar.getEngineAdvisers())
        .engineFacilitators(OrchestrationStepsModuleFacilitatorRegistrar.getEngineFacilitators())
        .engineEventHandlersMap(engineEventHandlersMap)
        .build();
  }

  protected Set<Class<? extends KryoRegistrar>> getKryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(ManagerRegistrars.kryoRegistrars)
        .add(TestManagerKryoRegistrar.class)
        .build();
  }

  protected Set<Class<? extends MorphiaRegistrar>> getMorphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(ManagerRegistrars.morphiaRegistrars)
        .add(ManagerTestMorphiaRegistrar.class)
        .build();
  }

  protected void registerProviders() {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  protected void registerObservers() {
    ObserversHelper.registerSharedObservers(injector);
  }

  protected void addQueueModules(List<Module> modules) {
    modules.add(new ManagerQueueModule());
  }

  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setUrl(PORTAL_URL);
    configuration.getPortal().setVerificationUrl(VERIFICATION_PATH);
    configuration.getPortal().setJwtExternalServiceSecret("JWT_EXTERNAL_SERVICE_SECRET");
    configuration.getPortal().setJwtPasswordSecret(JWT_PASSWORD_SECRET);
    configuration.getPortal().setJwtNextGenManagerSecret("dummy_key");
    configuration.getPortal().setOptionalDelegateTaskRejectAtLimit(10000);
    configuration.getPortal().setImportantDelegateTaskRejectAtLimit(50000);
    configuration.getPortal().setCriticalDelegateTaskRejectAtLimit(100000);
    configuration.setApiUrl("http:localhost:8080");
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(getProperty("setupScheduler", "false"));
    configuration.getServiceSchedulerConfig().setAutoStart(getProperty("setupScheduler", "false"));

    configuration.setGrpcDelegateServiceClientConfig(
        GrpcClientConfig.builder().target("localhost:9880").authority("localhost").build());
    configuration.setGrpcClientConfig(
        GrpcClientConfig.builder().target("localhost:9880").authority("localhost").build());

    configuration.setLogStreamingServiceConfig(
        LogStreamingServiceConfig.builder().baseUrl("http://localhost:8079").serviceToken("token").build());

    MarketPlaceConfig marketPlaceConfig =
        MarketPlaceConfig.builder().azureMarketplaceAccessKey("qwertyu").azureMarketplaceSecretKey("qwertyu").build();
    configuration.setMarketPlaceConfig(marketPlaceConfig);

    JobsFrequencyConfig jobsFrequencyConfig = JobsFrequencyConfig.builder().build();
    configuration.setJobsFrequencyConfig(jobsFrequencyConfig);

    if (annotations.stream().anyMatch(SetupScheduler.class ::isInstance)) {
      configuration.getBackgroundSchedulerConfig().setAutoStart("true");
      configuration.getServiceSchedulerConfig().setAutoStart("true");
      if (!annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
        configuration.getBackgroundSchedulerConfig().setJobStoreClass(
            org.quartz.simpl.RAMJobStore.class.getCanonicalName());
        configuration.getServiceSchedulerConfig().setJobStoreClass(
            org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
    }

    MarketoConfig marketoConfig =
        MarketoConfig.builder().clientId("client_id").clientSecret("client_secret_id").enabled(false).build();
    configuration.setMarketoConfig(marketoConfig);

    SegmentConfig segmentConfig = SegmentConfig.builder().enabled(false).url("dummy_url").apiKey("dummy_key").build();
    configuration.setSegmentConfig(segmentConfig);

    ServiceHttpClientConfig ngManagerServiceHttpClientConfig =
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build();
    configuration.setNgManagerServiceHttpClientConfig(ngManagerServiceHttpClientConfig);

    configuration.setDistributedLockImplementation(DistributedLockImplementation.NOOP);
    configuration.setEventsFrameworkConfiguration(
        EventsFrameworkConfiguration.builder()
            .redisConfig(RedisConfig.builder().redisUrl("dummyRedisUrl").build())
            .build());

    configuration.setTimeScaleDBConfig(TimeScaleDBConfig.builder().build());
    return configuration;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(executorService);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EventEmitter.class).toInstance(mock(EventEmitter.class));
        bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
        bind(MetricRegistry.class);
        bind(CommandLibraryServiceHttpClient.class).toInstance(mock(CommandLibraryServiceHttpClient.class));
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      PublisherConfiguration publisherConfiguration() {
        return PublisherConfiguration.allOn();
      }
    });

    modules.add(new LicenseModule());
    modules.add(new ValidationModule(validatorFactory));
    modules.add(TestMongoModule.getInstance());
    modules.add(new SpringPersistenceTestModule());
    modules.add(new DelegateServiceModule());
    modules.add(new WingsModule((MainConfiguration) configuration));
    modules.add(new IndexMigratorModule());
    modules.add(new YamlModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new WingsTestModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule((MainConfiguration) configuration));
    modules.add(new SSOModule());
    modules.add(new AuthModule());
    modules.add(new SignupModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    modules.add(new ManagerGrpcClientModule(
        ManagerGrpcClientModule.Config.builder()
            .target(((MainConfiguration) configuration).getGrpcClientConfig().getTarget())
            .authority(((MainConfiguration) configuration).getGrpcClientConfig().getAuthority())
            .build()));

    return modules;
  }

  private void registerListeners(java.util.Optional<Annotation> listenerOptional) {
    if (listenerOptional.isPresent()) {
      for (Class<? extends QueueListener> queueListenerClass : ((Listeners) listenerOptional.get()).value()) {
        if (queueListenerClass.equals(GeneralNotifyEventListener.class)) {
          final QueuePublisher<NotifyEvent> publisher =
              injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
          final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
              injector.getInstance(NotifyQueuePublisherRegister.class);
          notifyQueuePublisherRegister.register(GENERAL, payload -> publisher.send(asList(GENERAL), payload));
        } else if (queueListenerClass.equals(OrchestrationNotifyEventListener.class)) {
          final QueuePublisher<NotifyEvent> publisher =
              injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
          final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
              injector.getInstance(NotifyQueuePublisherRegister.class);
          notifyQueuePublisherRegister.register(
              ORCHESTRATION, payload -> publisher.send(asList(ORCHESTRATION), payload));
        }
        injector.getInstance(QueueListenerController.class).register(injector.getInstance(queueListenerClass), 1);
      }
    }
  }

  /**
   * After.
   *
   * @param annotations the annotations
   */
  protected void after(List<Annotation> annotations) {
    try {
      log.info("Stopping executorService...");
      executorService.shutdownNow();
      log.info("Stopped executorService...");
    } catch (Exception ex) {
      log.error("", ex);
    }

    try {
      log.info("Stopping notifier...");
      ((Managed) injector.getInstance(NotifierScheduledExecutorService.class)).stop();
      log.info("Stopped notifier...");
    } catch (Exception ex) {
      log.error("", ex);
    }

    try {
      log.info("Stopping queue listener controller...");
      injector.getInstance(QueueListenerController.class).stop();
      log.info("Stopped queue listener controller...");
    } catch (Exception ex) {
      log.error("", ex);
    }

    log.info("Stopping servers...");
    closingFactory.stopServers();
  }

  protected void registerScheduledJobs(Injector injector) {
    log.info("Initializing scheduledJobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(injector.getInstance(NotifyResponseCleaner.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }
}
