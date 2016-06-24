package software.wings.rules;

import static software.wings.app.LoggingInitializer.initializeLogging;

import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
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
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.CurrentThreadExecutor;
import software.wings.app.DatabaseModule;
import software.wings.app.ExecutorModule;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.WingsModule;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsPersistence;
import software.wings.lock.ManagedDistributedLockSvc;
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
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsRule implements MethodRule {
  private MongodExecutable mongodExecutable;
  private Injector injector;
  private MongoServer mongoServer;
  private Datastore datastore;
  private DistributedLockSvc distributedLockSvc;
  private int port = 0;
  private ExecutorService executorService = new CurrentThreadExecutor();

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
        WingsRule.this.before(annotations, target.getClass().getSimpleName() + "." + frameworkMethod.getName());
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          WingsRule.this.after();
        }
      }
    };
  }

  /**
   * Gets datastore.
   *
   * @return the datastore
   */
  public Datastore getDatastore() {
    return datastore;
  }

  /**
   * Before.
   *
   * @param annotations the annotations
   * @throws Throwable the throwable
   */
  protected void before(List<Annotation> annotations, String testName) throws Throwable {
    initializeLogging();

    MongoClient mongoClient;
    if (annotations.stream().filter(annotation -> Integration.class.isInstance(annotation)).findFirst().isPresent()) {
      try {
        port = Integer.parseInt(System.getProperty("mongoPort", "27017"));
      } catch (NumberFormatException ex) {
        port = 27017;
      }
      mongoClient = new MongoClient("localhost", port);
    } else {
      if (annotations.stream().filter(annotation -> RealMongo.class.isInstance(annotation)).findFirst().isPresent()) {
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                                           .defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(RealMongo.class))
                                           .build();
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

        int port = Network.getFreeServerPort();
        IMongodConfig mongodConfig =
            new MongodConfigBuilder().version(Main.V3_2).net(new Net(port, Network.localhostIsIPv6())).build();
        mongodExecutable = starter.prepare(mongodConfig);
        mongodExecutable.start();
        mongoClient = new MongoClient("localhost", port);
      } else {
        mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind("localhost", port);
        InetSocketAddress serverAddress = mongoServer.getLocalAddress();
        mongoClient = new MongoClient(new ServerAddress(serverAddress));
      }
    }

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = morphia.createDatastore(mongoClient, "wings");
    distributedLockSvc = new ManagedDistributedLockSvc(
        new DistributedLockSvcFactory(new DistributedLockSvcOptions(mongoClient, "wings", "locks")).getLockSvc());
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }

    MainConfiguration configuration = new MainConfiguration();
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    injector = Guice.createInjector(new ValidationModule(validatorFactory),
        new DatabaseModule(datastore, datastore, distributedLockSvc), new WingsModule(configuration),
        new ExecutorModule(executorService), new QueueModule(datastore));

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
   */
  protected void after() {
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
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 5000L, TimeUnit.MILLISECONDS);
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
