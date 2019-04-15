package software.wings.rules;

import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.maintenance.MaintenanceController.forceMaintenance;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.harness.event.EventsModule;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.exception.WingsException;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListener;
import io.harness.queue.QueueListenerController;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.rule.BypassRuleMixin;
import io.harness.rule.DistributedLockRuleMixin;
import io.harness.rule.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.waiter.Notifier;
import io.harness.waiter.NotifierScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.internal.util.MockUtil;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.WingsTestModule;
import software.wings.app.CacheModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsApplication;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.integration.BaseIntegrationTest;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.security.authentication.oauth.AzureConfig;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.GithubConfig;
import software.wings.security.authentication.oauth.GitlabConfig;
import software.wings.security.authentication.oauth.GoogleConfig;
import software.wings.security.authentication.oauth.LinkedinConfig;
import software.wings.service.impl.EventEmitter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class WingsRule implements MethodRule, BypassRuleMixin, MongoRuleMixin, DistributedLockRuleMixin {
  protected ClosingFactory closingFactory = new ClosingFactory();

  protected Injector injector;
  protected AdvancedDatastore datastore;
  private int port;
  private ExecutorService executorService = new CurrentThreadExecutor();
  protected MongoType mongoType;

  /* (non-Javadoc)
   * @see org.junit.rules.MethodRule#apply(org.junit.runners.model.Statement, org.junit.runners.model.FrameworkMethod,
   * java.lang.Object)
   */
  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    if (bypass(frameworkMethod)) {
      return noopStatement();
    }

    Statement wingsStatement = new Statement() {
      @Override
      public void evaluate() throws Throwable {
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
    };

    return wingsStatement;
  }

  protected boolean isIntegrationTest(Object target) {
    return target instanceof BaseIntegrationTest;
  }

  /**
   * Gets datastore.
   *
   * @return the datastore
   */
  public AdvancedDatastore getDatastore() {
    return datastore;
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
    initializeLogging();
    forceMaintenance(false);
    MongoClient mongoClient;
    String dbName = System.getProperty("dbName", "harness");

    if (annotations.stream().anyMatch(Integration.class ::isInstance) || doesExtendBaseIntegrationTest) {
      try {
        MongoClientURI clientUri = new MongoClientURI(
            System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName), mongoClientOptions);
        if (!clientUri.getURI().startsWith("mongodb://localhost:")) {
          forceMaintenance(true);
          // Protection against running tests on non-local databases such as prod or qa.
          // Comment out this throw exception if you're sure.
          throw new WingsException("\n*** WARNING *** : Attempting to run test on non-local Mongo: "
              + clientUri.getURI() + "\n*** Exiting *** : Comment out this check in WingsRule.java "
              + "if you are sure you want to run against a remote Mongo.\n");
        }
        dbName = clientUri.getDatabase();
        mongoClient = new MongoClient(clientUri);
        closingFactory.addServer(mongoClient);
      } catch (NumberFormatException ex) {
        port = 27017;
        mongoClient = new MongoClient("localhost", port);
        closingFactory.addServer(mongoClient);
      }
    } else {
      final MongoInfo mongoInfo = testMongo(annotations, closingFactory);
      mongoClient = mongoInfo.getClient();
      mongoType = mongoInfo.getType();
    }

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());

    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, dbName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoClient, dbName, closingFactory);

    Configuration configuration = getConfiguration(annotations, dbName);

    HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);

    List<Module> modules = getRequiredModules(configuration, distributedLockSvc);
    addQueueModules(modules);

    if (annotations.stream().filter(annotation -> Cache.class.isInstance(annotation)).findFirst().isPresent()) {
      System.setProperty("hazelcast.jcache.provider.type", "server");
      CacheModule cacheModule = new CacheModule((MainConfiguration) configuration);
      modules.add(0, cacheModule);
      hazelcastInstance = cacheModule.getHazelcastInstance();
    }

    if (annotations.stream()
            .filter(annotation -> Hazelcast.class.isInstance(annotation) || Cache.class.isInstance(annotation))
            .findFirst()
            .isPresent()) {
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

    registerListeners(annotations.stream().filter(annotation -> Listeners.class.isInstance(annotation)).findFirst());
    registerScheduledJobs(injector);
    registerProviders();
    registerObservers();
  }

  protected void registerProviders() {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  protected void registerObservers() {
    WingsApplication.registerObservers(injector);
  }

  protected void addQueueModules(List<Module> modules) {
    modules.add(new ManagerQueueModule());
  }

  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setAllowedDomains("wings.software");
    configuration.getPortal().setUrl(PORTAL_URL);
    configuration.getPortal().setVerificationUrl(VERIFICATION_PATH);
    configuration.getPortal().setJwtExternalServiceSecret("JWT_EXTERNAL_SERVICE_SECRET");
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    configuration.getServiceSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    if (annotations.stream().anyMatch(SetupScheduler.class ::isInstance)) {
      configuration.getBackgroundSchedulerConfig().setAutoStart("true");
      configuration.getServiceSchedulerConfig().setAutoStart("true");
      if (mongoType == MongoType.FAKE) {
        configuration.getBackgroundSchedulerConfig().setJobStoreClass(
            org.quartz.simpl.RAMJobStore.class.getCanonicalName());
        configuration.getServiceSchedulerConfig().setJobStoreClass(
            org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
    }

    MarketoConfig marketoConfig =
        MarketoConfig.builder().clientId("client_id").clientSecret("client_secret_id").enabled(false).build();

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    LinkedinConfig linkedinConfig =
        LinkedinConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GoogleConfig googleConfig =
        GoogleConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GitlabConfig gitlabConfig =
        GitlabConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GithubConfig githubConfig =
        GithubConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    BitbucketConfig bitbucketConfig =
        BitbucketConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();

    configuration.setMarketoConfig(marketoConfig);
    configuration.setAzureConfig(azureConfig);
    configuration.setLinkedinConfig(linkedinConfig);
    configuration.setGoogleConfig(googleConfig);
    configuration.setBitbucketConfig(bitbucketConfig);
    configuration.setGithubConfig(githubConfig);
    configuration.setGitlabConfig(gitlabConfig);
    return configuration;
  }

  protected List<Module> getRequiredModules(Configuration configuration, DistributedLockSvc distributedLockSvc) {
    ExecutorModule.getInstance().setExecutorService(executorService);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList();

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EventEmitter.class).toInstance(mock(EventEmitter.class));
        bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
        bind(MetricRegistry.class);
      }
    });

    modules.add(new LicenseModule());
    modules.add(new ValidationModule(validatorFactory));
    modules.add(new MongoModule(datastore, datastore, distributedLockSvc));
    modules.addAll(new WingsModule((MainConfiguration) configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new WingsTestModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule((MainConfiguration) configuration));
    return modules;
  }

  private void registerListeners(java.util.Optional<Annotation> listenerOptional) {
    if (listenerOptional.isPresent()) {
      for (Class<? extends QueueListener> queueListenerClass : ((Listeners) listenerOptional.get()).value()) {
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
    // Clear caches.
    if (annotations.stream()
            .filter(annotation -> Hazelcast.class.isInstance(annotation) || Cache.class.isInstance(annotation))
            .findFirst()
            .isPresent()) {
      CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
      cacheManager.getCacheNames().forEach(s -> cacheManager.destroyCache(s));
    }

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

    try {
      log().info("Stopping timer...");
      ((Managed) injector.getInstance(TimerScheduledExecutorService.class)).stop();
      log().info("Stopped timer...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    log().info("Stopping servers...");
    closingFactory.stopServers();
  }

  protected void registerScheduledJobs(Injector injector) {
    log().info("Initializing scheduledJobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }

  protected Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
