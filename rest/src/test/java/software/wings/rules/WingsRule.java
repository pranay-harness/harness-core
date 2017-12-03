package software.wings.rules;

import static org.mockito.Mockito.mock;
import static software.wings.app.LoggingInitializer.initializeLogging;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import io.dropwizard.lifecycle.Managed;
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
import software.wings.CurrentThreadExecutor;
import software.wings.WingsTestModule;
import software.wings.app.CacheModule;
import software.wings.app.DatabaseModule;
import software.wings.app.ExecutorModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.service.impl.EventEmitter;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.utils.NoDefaultConstructorMorphiaObjectFactory;
import software.wings.utils.ThreadContext;
import software.wings.waitnotify.Notifier;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsRule implements MethodRule {
  private static IRuntimeConfig runtimeConfig =
      new RuntimeConfigBuilder().defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(RealMongo.class)).build();

  private static MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

  private MongodExecutable mongodExecutable;
  private Injector injector;
  private MongoServer mongoServer;
  private AdvancedDatastore datastore;
  private DistributedLockSvc distributedLockSvc;
  private int port = 0;
  private ExecutorService executorService = new CurrentThreadExecutor();
  private boolean fakeMongo;

  /* (non-Javadoc)
   * @see org.junit.rules.MethodRule#apply(org.junit.runners.model.Statement, org.junit.runners.model.FrameworkMethod,
   * java.lang.Object)
   */
  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        List<Annotation> annotations = Lists.newArrayList(Arrays.asList(frameworkMethod.getAnnotations()));
        annotations.addAll(Arrays.asList(target.getClass().getAnnotations()));
        WingsRule.this.before(annotations, target instanceof BaseIntegrationTest,
            target.getClass().getSimpleName() + "." + frameworkMethod.getName());
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          WingsRule.this.after(annotations);
        }
      }
    };
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
    MaintenanceController.forceMaintenanceOff();
    MongoClient mongoClient;
    String dbName = "harness";
    if (annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
      int port = Network.getFreeServerPort();
      IMongodConfig mongodConfig = new MongodConfigBuilder()
                                       .version(Main.V3_2)
                                       .net(new Net("127.0.0.1", port, Network.localhostIsIPv6()))
                                       .build();
      mongodExecutable = starter.prepare(mongodConfig);
      mongodExecutable.start();
      mongoClient = new MongoClient("localhost", port);
    } else if (annotations.stream().anyMatch(Integration.class ::isInstance) || doesExtendBaseIntegrationTest) {
      try {
        MongoClientURI clientUri =
            new MongoClientURI(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName));
        // Protection against running tests on non-local databases such as prod or qa. Comment out this check if you're
        // sure.
        if (!clientUri.getURI().startsWith("mongodb://localhost:")) {
          System.out.println("\n*** WARNING *** : Attempting to run test on non-local Mongo: " + clientUri.getURI());
          System.out.println(
              "*** Exiting *** : Comment out this check in WingsRule.java if you are sure you want to run against a remote Mongo.\n");
          System.exit(1);
        }
        dbName = clientUri.getDatabase();
        mongoClient = new MongoClient(clientUri);
      } catch (NumberFormatException ex) {
        port = 27017;
        mongoClient = new MongoClient("localhost", port);
      }
    } else {
      fakeMongo = true;
      mongoServer = new MongoServer(new MemoryBackend());
      mongoServer.bind("localhost", port);
      InetSocketAddress serverAddress = mongoServer.getLocalAddress();
      mongoClient = new MongoClient(new ServerAddress(serverAddress));
    }

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, dbName);
    DistributedLockSvcOptions distributedLockSvcOptions = new DistributedLockSvcOptions(mongoClient, dbName, "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    distributedLockSvc =
        new ManagedDistributedLockSvc(new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc());
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }

    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setAllowedDomains("wings.software");
    configuration.getPortal().setUrl(PORTAL_URL);
    configuration.getPortal().setVerificationUrl(VERIFICATION_PATH);
    configuration.getMongoConnectionFactory().setUri(
        System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName));
    configuration.getSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    if (annotations.stream().anyMatch(SetupScheduler.class ::isInstance)) {
      configuration.getSchedulerConfig().setAutoStart("true");
    }

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    HazelcastInstance hazelcastInstance = mock(HazelcastInstance.class);

    List<AbstractModule> modules = Lists.newArrayList(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(EventEmitter.class).toInstance(mock(EventEmitter.class));
            bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
            bind(MetricRegistry.class);
          }
        },
        new LicenseModule(), new ValidationModule(validatorFactory),
        new DatabaseModule(datastore, datastore, distributedLockSvc), new WingsModule(configuration), new YamlModule(),
        new ExecutorModule(executorService), new WingsTestModule());

    if (fakeMongo) {
      modules.add(new QueueModuleTest(datastore));
    } else {
      modules.add(new QueueModule(datastore));
    }

    if (annotations.stream().filter(annotation -> Cache.class.isInstance(annotation)).findFirst().isPresent()) {
      System.setProperty("hazelcast.jcache.provider.type", "server");
      CacheModule cacheModule = new CacheModule(configuration);
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

    ThreadContext.setContext(testName + "-");
    registerListeners(annotations.stream().filter(annotation -> Listeners.class.isInstance(annotation)).findFirst());
    registerScheduledJobs(injector);
  }

  private void registerListeners(java.util.Optional<Annotation> listenerOptional) {
    if (listenerOptional.isPresent()) {
      for (Class<? extends AbstractQueueListener> queueListenerClass : ((Listeners) listenerOptional.get()).value()) {
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
      ex.printStackTrace();
    }

    try {
      log().info("Stopping notifier...");
      ((Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))).stop();
      log().info("Stopped notifier...");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      log().info("Stopping queue listener controller...");
      injector.getInstance(QueueListenerController.class).stop();
      log().info("Stopped queue listener controller...");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      log().info("Stopping timer...");
      ((Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("timer")))).stop();
      log().info("Stopped timer...");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      log().info("Stopping distributed lock service...");
      ((Managed) distributedLockSvc).stop();
      log().info("Stopped distributed lock service...");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    try {
      log().info("Stopping WingsPersistance...");
      ((Managed) injector.getInstance(WingsPersistence.class)).stop();
      log().info("Stopped WingsPersistance...");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    log().info("Stopping Mongo server...");
    if (mongoServer != null) {
      mongoServer.shutdown();
    }
    if (mongodExecutable != null) {
      mongodExecutable.stop();
    }

    log().info("Stopped Mongo server...");
  }

  private void registerScheduledJobs(Injector injector) {
    log().info("Initializing scheduledJobs...");
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
