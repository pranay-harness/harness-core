package software.wings.rules;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.ifesdjeen.timer.HashedWheelTimer;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.dropwizard.lifecycle.Managed;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.WingsBootstrap;
import software.wings.beans.ReadPref;
import software.wings.common.thread.ForceQueuePolicy;
import software.wings.common.thread.ScalingQueue;
import software.wings.common.thread.ScalingThreadPoolExecutor;
import software.wings.core.queue.*;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.service.impl.*;
import software.wings.service.intfc.*;
import software.wings.utils.CurrentThreadExecutor;
import software.wings.utils.ManagedScheduledExecutorService;
import software.wings.waitNotify.NotifyEvent;
import software.wings.waitNotify.NotifyEventListener;

import javax.inject.Named;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsRule implements MethodRule {
  private Injector injector;
  private MongoServer mongoServer;
  private Datastore datastore;
  private DistributedLockSvc distributedLockSvc;
  private int port = 0;
  private ExecutorService executorService = new CurrentThreadExecutor();

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      public void evaluate() throws Throwable {
        WingsRule.this.before();
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          WingsRule.this.after();
        }
      }
    };
  }

  protected void before() throws Throwable {
    mongoServer = new MongoServer(new MemoryBackend());
    mongoServer.bind("localhost", port);
    InetSocketAddress serverAddress = mongoServer.getLocalAddress();
    final MongoClient client = new MongoClient(new ServerAddress(serverAddress));

    Morphia morphia = new Morphia();
    datastore = morphia.createDatastore(client, "wings");
    distributedLockSvc = new ManagedDistributedLockSvc(
        new DistributedLockSvcFactory(new DistributedLockSvcOptions(client, "wings", "locks")).getLockSvc());
    if (!distributedLockSvc.isRunning()) {
      distributedLockSvc.startup();
    }

    injector = Guice.createInjector(new ValidationModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(Datastore.class).annotatedWith(Names.named("primaryDatastore")).toInstance(datastore);
        bind(Datastore.class).annotatedWith(Names.named("secondaryDatastore")).toInstance(datastore);
        bind(new TypeLiteral<Map<ReadPref, Datastore>>() {})
            .annotatedWith(Names.named("datastoreMap"))
            .toInstance(ImmutableMap.<ReadPref, Datastore>of(ReadPref.CRITICAL, datastore, ReadPref.NORMAL, datastore));
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
        bind(ArtifactService.class).to(ArtifactServiceImpl.class);
        bind(WorkflowService.class).to(WorkflowServiceImpl.class);
        bind(RoleService.class).to(RoleServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
        bind(ExecutorService.class).toInstance(executorService);
        bind(InfraService.class).to(InfraServiceImpl.class);
        bind(DistributedLockSvc.class).toInstance(distributedLockSvc);
        bind(MongoClient.class).toInstance(client);
        bind(new TypeLiteral<Queue<NotifyEvent>>() {}).toInstance(new MongoQueueImpl<>(NotifyEvent.class, datastore));
        bind(new TypeLiteral<AbstractQueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
        bind(ScheduledExecutorService.class)
            .annotatedWith(Names.named("timer"))
            .toInstance(new ManagedScheduledExecutorService(new HashedWheelTimer()));
      }
    });
    injector.getInstance(QueueListenerController.class).register(injector.getInstance(NotifyEventListener.class), 1);
    WingsBootstrap.initialize(injector);
  }

  public Datastore getDatastore() {
    return datastore;
  }

  protected void after() {
    try {
      log().info("Stopping timer...");
      ((Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("timer")))).stop();
      executorService.shutdownNow();
      log().info("Stopped timer...");
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      log().info("Stopping queue listener controller...");
      injector.getInstance(QueueListenerController.class).stop();
      log().info("Stopped queue listener controller...");
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      log().info("Stopping distributed lock service...");
      ((Managed) distributedLockSvc).stop();
      log().info("Stopped distributed lock service...");
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      log().info("Stopping WingsPersistance...");
      ((Managed) injector.getInstance(WingsPersistence.class)).stop();
      log().info("Stopped WingsPersistance...");
      log().info("Stopped distributed lock service...");
    } catch (Exception e) {
      e.printStackTrace();
    }

    log().info("Stopping Mongo server...");
    mongoServer.shutdown();
    log().info("Stopped Mongo server...");
  }

  private Logger log() {
    return LoggerFactory.getLogger(getClass());
  }
}
