package software.wings.rules;

import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.maintenance.MaintenanceController.forceMaintenance;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

import com.codahale.metrics.MetricRegistry;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.config.PublisherConfiguration;
import io.harness.event.EventsModule;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.mongo.MongoConfig;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.RealMongo;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.internal.util.MockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.WingsTestModule;
import software.wings.app.AuthModule;
import software.wings.app.CacheModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.SSOModule;
import software.wings.app.SignupModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsApplication;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.integration.BaseIntegrationTest;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.service.impl.EventEmitter;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

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
    return target instanceof BaseIntegrationTest;
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
    System.setProperty("javax.cache.spi.CachingProvider", "com.hazelcast.cache.HazelcastCachingProvider");
    initializeLogging();
    forceMaintenance(false);
    MongoClient mongoClient;
    String dbName = System.getProperty("dbName", "harness");

    configuration = getConfiguration(annotations, dbName);

    HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);

    List<Module> modules = modules(annotations);
    addQueueModules(modules);

    if (annotations.stream().anyMatch(Cache.class ::isInstance)) {
      System.setProperty("hazelcast.jcache.provider.type", "server");
      CacheModule cacheModule = new CacheModule((MainConfiguration) configuration);
      modules.add(0, cacheModule);
      hazelcastInstance = cacheModule.getHazelcastInstance();
    }

    if (annotations.stream().anyMatch(annotation -> annotation instanceof Hazelcast || annotation instanceof Cache)) {
      if (new MockUtil().isMock(hazelcastInstance)) {
        hazelcastInstance = com.hazelcast.core.Hazelcast.newHazelcastInstance();
      }
    }

    HazelcastInstance finalHazelcastInstance = hazelcastInstance;

    modules.add(0, new AbstractModule() {
      @Override
      protected void configure() {
        bind(HazelcastInstance.class).toInstance(finalHazelcastInstance);
      }
    });

    injector = Guice.createInjector(modules);

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

  protected void registerProviders() {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  protected void registerObservers() {
    WingsApplication.registerSharedObservers(injector);
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
    configuration.setApiUrl("http:localhost:8080");
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    configuration.getServiceSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));

    MarketPlaceConfig marketPlaceConfig =
        MarketPlaceConfig.builder().azureMarketplaceAccessKey("qwertyu").azureMarketplaceSecretKey("qwertyu").build();
    configuration.setMarketPlaceConfig(marketPlaceConfig);

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

    configuration.setDistributedLockImplementation(DistributedLockImplementation.NOOP);
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
    modules.addAll(new TestMongoModule().cumulativeDependencies());
    modules.addAll(new WingsModule((MainConfiguration) configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new WingsTestModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule((MainConfiguration) configuration));
    modules.add(new SSOModule());
    modules.add(new AuthModule());
    modules.add(new SignupModule());
    modules.add(new GcpMarketplaceIntegrationModule());

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
      log().info("Stopping executorService...");
      executorService.shutdownNow();
      log().info("Stopped executorService...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    try {
      log().info("Stopping notifier...");
      ((Managed) injector.getInstance(NotifierScheduledExecutorService.class)).stop();
      log().info("Stopped notifier...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    try {
      log().info("Stopping queue listener controller...");
      injector.getInstance(QueueListenerController.class).stop();
      log().info("Stopped queue listener controller...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    log().info("Stopping servers...");
    closingFactory.stopServers();
  }

  protected void registerScheduledJobs(Injector injector) {
    log().info("Initializing scheduledJobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(injector.getInstance(NotifyResponseCleaner.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }

  protected Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
