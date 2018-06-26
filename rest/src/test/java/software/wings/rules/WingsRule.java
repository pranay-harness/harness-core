package software.wings.rules;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static software.wings.app.DatabaseModule.mongoClientOptions;
import static software.wings.app.LoggingInitializer.initializeLogging;
import static software.wings.core.maintenance.MaintenanceController.forceMaintenance;
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
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
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
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.cglib.CGLibProxySerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import io.dropwizard.lifecycle.Managed;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.internal.util.MockUtil;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.CurrentThreadExecutor;
import software.wings.WingsTestModule;
import software.wings.app.CacheModule;
import software.wings.app.DatabaseModule;
import software.wings.app.ExecutorModule;
import software.wings.app.HQueryFactory;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.WingsApplication;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.integration.BaseIntegrationTest;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.service.impl.EventEmitter;
import software.wings.utils.KryoUtils;
import software.wings.utils.NoDefaultConstructorMorphiaObjectFactory;
import software.wings.utils.ThreadContext;
import software.wings.waitnotify.Notifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.net.InetSocketAddress;
import java.util.GregorianCalendar;
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
  private static final Logger logger = LoggerFactory.getLogger(WingsRule.class);

  private static IRuntimeConfig runtimeConfig =
      new RuntimeConfigBuilder().defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(RealMongo.class)).build();

  private static MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

  private MongodExecutable mongodExecutable;
  private Injector injector;
  private MongoServer mongoServer;
  private AdvancedDatastore datastore;
  private DistributedLockSvc distributedLockSvc;
  private int port;
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
        List<Annotation> annotations = Lists.newArrayList(asList(frameworkMethod.getAnnotations()));
        annotations.addAll(asList(target.getClass().getAnnotations()));
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
    setKryoClassRegistrationForTests();
    initializeLogging();
    forceMaintenance(false);
    MongoClient mongoClient;
    String dbName = "harness";
    if (annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
      mongoClient = getRandomPortMongoClient();
    } else if (annotations.stream().anyMatch(Integration.class ::isInstance) || doesExtendBaseIntegrationTest) {
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
    datastore.setQueryFactory(new HQueryFactory());
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
      if (fakeMongo) {
        configuration.getSchedulerConfig().setJobstoreclass(org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
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
    WingsApplication.registerObservers(injector);
  }

  private MongoClient getRandomPortMongoClient() throws Exception {
    Exception persistent = null;

    // FreeServerPort releases the port before it returns it. This creates a race between the moment it is obtain again
    // and reserved for mongo. In rare cases this can cause the function to fail with port already in use exception.
    //
    // There is no good way to eliminate the race, since the port must be free mongo to be able to grab it.
    //
    // Lets retry a number of times to reduce the likelihood almost to zero.
    for (int i = 0; i < 20; i++) {
      int port = Network.getFreeServerPort();
      IMongodConfig mongodConfig = new MongodConfigBuilder()
                                       .version(Main.V3_6)
                                       .net(new Net("127.0.0.1", port, Network.localhostIsIPv6()))
                                       .build();
      try {
        // It seems that the starter is not thread safe. We still have a likelihood for multiprocessor problems
        // but lets at least do what is cheap to have.
        synchronized (starter) {
          mongodExecutable = starter.prepare(mongodConfig);
        }

        mongodExecutable.start();
        return new MongoClient("localhost", port);
      } catch (Exception e) {
        // Note this handles race int the port, but also in the starter prepare
        Thread.sleep(250);
        persistent = e;
      }
    }

    throw persistent;
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
      logger.error("", ex);
    }

    try {
      log().info("Stopping notifier...");
      ((Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))).stop();
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
      ((Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("timer")))).stop();
      log().info("Stopped timer...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    try {
      log().info("Stopping distributed lock service...");
      if (distributedLockSvc instanceof Managed) {
        ((Managed) distributedLockSvc).stop();
      }
      log().info("Stopped distributed lock service...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    try {
      log().info("Stopping WingsPersistence...");
      ((Managed) injector.getInstance(WingsPersistence.class)).stop();
      log().info("Stopped WingsPersistence...");
    } catch (Exception ex) {
      logger.error("", ex);
    }

    log().info("Stopping Mongo server...");
    if (mongoServer != null) {
      mongoServer.shutdown();
    }
    try {
      if (mongodExecutable != null) {
        mongodExecutable.stop();
      }
    } catch (IllegalStateException ise) {
      // we are   swallowing this - couldn't kill the embedded mongod process, but we don't care
      log().info("Had issues stopping embedded mongod: {}", ise.getMessage());
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

  private void setKryoClassRegistrationForTests() {
    KryoUtils.setKryoPoolForTests(
        new KryoPool
            .Builder(() -> {
              Kryo kryo = new Kryo();
              // Log.TRACE();
              kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
              kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
                  FieldSerializer.CachedFieldNameStrategy.EXTENDED);
              kryo.getFieldSerializerConfig().setCopyTransient(false);
              kryo.register(asList("").getClass(), new ArraysAsListSerializer());
              kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
              kryo.register(InvocationHandler.class, new JdkProxySerializer());
              UnmodifiableCollectionsSerializer.registerSerializers(kryo);
              SynchronizedCollectionsSerializer.registerSerializers(kryo);

              // custom serializers for non-jdk libs

              // register CGLibProxySerializer, works in combination with the appropriate action in
              // handleUnregisteredClass (see below)
              kryo.register(CGLibProxySerializer.CGLibProxyMarker.class, new CGLibProxySerializer());
              // guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, ReverseList,
              // UnmodifiableNavigableSet
              ImmutableListSerializer.registerSerializers(kryo);
              ImmutableSetSerializer.registerSerializers(kryo);
              ImmutableMapSerializer.registerSerializers(kryo);
              ImmutableMultimapSerializer.registerSerializers(kryo);
              ReverseListSerializer.registerSerializers(kryo);
              UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
              // guava ArrayListMultimap, HashMultimap, LinkedHashMultimap, LinkedListMultimap, TreeMultimap
              ArrayListMultimapSerializer.registerSerializers(kryo);
              HashMultimapSerializer.registerSerializers(kryo);
              LinkedHashMultimapSerializer.registerSerializers(kryo);
              LinkedListMultimapSerializer.registerSerializers(kryo);
              TreeMultimapSerializer.registerSerializers(kryo);
              return kryo;
            })
            .softReferences()
            .build());
  }
}
