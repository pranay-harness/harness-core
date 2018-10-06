package software.wings.rules;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static software.wings.app.LoggingInitializer.initializeLogging;
import static software.wings.core.maintenance.MaintenanceController.forceMaintenance;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.VERIFICATION_PATH;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.hazelcast.core.HazelcastInstance;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
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
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;
import io.harness.exception.WingsException;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.MongoModule;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.rule.DistributedLockRuleMixin;
import io.harness.rule.MongoRuleMixin;
import io.harness.rule.RealMongo;
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
import software.wings.app.ExecutorModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsApplication;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.EventEmitter;
import software.wings.utils.KryoUtils;
import software.wings.utils.ThreadContext;
import software.wings.waitnotify.Notifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class WingsRule implements MethodRule, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(WingsRule.class);

  private static IRuntimeConfig runtimeConfig =
      new RuntimeConfigBuilder()
          .defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(RealMongo.class))
          .artifactStore(new ExtractedArtifactStoreBuilder()
                             .defaults(Command.MongoD)
                             .download(new DownloadConfigBuilder()
                                           .defaultsForCommand(Command.MongoD)
                                           .downloadPath("https://storage.googleapis.com/harness-tests/")))
          .build();

  private static MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

  protected ClosingFactory closingFactory = new ClosingFactory();

  protected MongodExecutable mongodExecutable;
  protected Injector injector;
  protected AdvancedDatastore datastore;
  private int port;
  private ExecutorService executorService = new CurrentThreadExecutor();
  protected boolean fakeMongo;

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
    setKryoClassRegistrationForTests();
    initializeLogging();
    forceMaintenance(false);
    MongoClient mongoClient;
    String dbName = System.getProperty("dbName", "harness");
    if (annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
      mongoClient = getRandomPortMongoClient();
      closingFactory.addServer(mongoClient);
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
        closingFactory.addServer(mongoClient);
      } catch (NumberFormatException ex) {
        port = 27017;
        mongoClient = new MongoClient("localhost", port);
        closingFactory.addServer(mongoClient);
      }
    } else {
      fakeMongo = true;
      mongoClient = fakeMongoClient(port, closingFactory);
    }

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
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

    ThreadContext.setContext(testName + "-");
    registerListeners(annotations.stream().filter(annotation -> Listeners.class.isInstance(annotation)).findFirst());
    registerScheduledJobs(injector);
    registerObservers();
  }

  protected void registerObservers() {
    WingsApplication.registerObservers(injector);
  }

  protected void addQueueModules(List<Module> modules) {
    if (fakeMongo) {
      modules.add(new QueueModuleTest());
    } else {
      modules.add(new QueueModule(false));
    }
  }

  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
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
    return configuration;
  }

  protected List<Module> getRequiredModules(Configuration configuration, DistributedLockSvc distributedLockSvc) {
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
    modules.add(new ExecutorModule(executorService));
    modules.add(new WingsTestModule());
    modules.add(new TemplateModule());
    return modules;
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

    log().info("Stopping servers...");
    closingFactory.stopServers();

    try {
      if (mongodExecutable != null) {
        mongodExecutable.stop();
      }
    } catch (IllegalStateException ise) {
      // we are   swallowing this - couldn't kill the embedded mongod process, but we don't care
      log().info("Had issues stopping embedded mongod: {}", ise.getMessage());
    }
  }

  protected void registerScheduledJobs(Injector injector) {
    log().info("Initializing scheduledJobs...");
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 1000L, TimeUnit.MILLISECONDS);
  }

  protected Logger log() {
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
