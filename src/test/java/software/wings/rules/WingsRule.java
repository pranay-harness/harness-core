package software.wings.rules;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.runtime.Network;
import io.dropwizard.lifecycle.Managed;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.WingsBootstrap;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.beans.ReadPref;
import software.wings.collect.ArtifactCollectEventListener;
import software.wings.collect.CollectEvent;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.MongoQueueImpl;
import software.wings.core.queue.Queue;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.Jenkins;
import software.wings.helpers.ext.JenkinsFactory;
import software.wings.helpers.ext.JenkinsImpl;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.service.impl.AppServiceImpl;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.impl.CatalogServiceImpl;
import software.wings.service.impl.ConfigServiceImpl;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.HostServiceImpl;
import software.wings.service.impl.InfraServiceImpl;
import software.wings.service.impl.JenkinsArtifactCollectorServiceImpl;
import software.wings.service.impl.JenkinsBuildServiceImpl;
import software.wings.service.impl.ReleaseServiceImpl;
import software.wings.service.impl.RoleServiceImpl;
import software.wings.service.impl.ServiceResourceServiceImpl;
import software.wings.service.impl.ServiceTemplateServiceImpl;
import software.wings.service.impl.TagServiceImpl;
import software.wings.service.impl.UserServiceImpl;
import software.wings.service.impl.WorkflowServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.RoleService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.CurrentThreadExecutor;
import software.wings.utils.ManagedScheduledExecutorService;
import software.wings.waitnotify.Notifier;
import software.wings.waitnotify.NotifyEvent;
import software.wings.waitnotify.NotifyEventListener;

import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        List<Annotation> annotations = Lists.newArrayList(Arrays.asList(frameworkMethod.getAnnotations()));
        annotations.addAll(Arrays.asList(target.getClass().getAnnotations()));
        WingsRule.this.before(annotations);
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          WingsRule.this.after();
        }
      }
    };
  }

  public Datastore getDatastore() {
    return datastore;
  }

  protected void before(List<Annotation> annotations) throws Throwable {
    MongoClient mongoClient;
    if (annotations.stream().filter(annotation -> Integration.class.isInstance(annotation)).findFirst().isPresent()) {
      mongoClient = new MongoClient("localhost", 27017);
    } else {
      if (annotations.stream().filter(annotation -> RealMongo.class.isInstance(annotation)).findFirst().isPresent()) {
        MongodStarter starter = MongodStarter.getDefaultInstance();

        int port = Network.getFreeServerPort();
        IMongodConfig mongodConfig =
            new MongodConfigBuilder().version(Main.V3_2).net(new Net(port, Network.localhostIsIPv6())).build();
        mongodExecutable = starter.prepare(mongodConfig);
        MongodProcess mongod = mongodExecutable.start();
        mongoClient = new MongoClient("localhost", port);
      } else {
        mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind("localhost", port);
        InetSocketAddress serverAddress = mongoServer.getLocalAddress();
        mongoClient = new MongoClient(new ServerAddress(serverAddress));
      }
    }

    Morphia morphia = new Morphia();
    datastore = morphia.createDatastore(mongoClient, "wings");
    distributedLockSvc = new ManagedDistributedLockSvc(
        new DistributedLockSvcFactory(new DistributedLockSvcOptions(mongoClient, "wings", "locks")).getLockSvc());
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
            .toInstance(ImmutableMap.of(ReadPref.CRITICAL, datastore, ReadPref.NORMAL, datastore));
        bind(WingsPersistence.class).to(WingsMongoPersistence.class);
        bind(ArtifactService.class).to(ArtifactServiceImpl.class);
        bind(WorkflowService.class).to(WorkflowServiceImpl.class);
        bind(RoleService.class).to(RoleServiceImpl.class);
        bind(UserService.class).to(UserServiceImpl.class);
        bind(ExecutorService.class).toInstance(executorService);
        bind(InfraService.class).to(InfraServiceImpl.class);
        bind(DistributedLockSvc.class).toInstance(distributedLockSvc);
        bind(MongoClient.class).toInstance(mongoClient);
        bind(new TypeLiteral<Queue<NotifyEvent>>() {}).toInstance(new MongoQueueImpl<>(NotifyEvent.class, datastore));
        bind(new TypeLiteral<AbstractQueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
        bind(ScheduledExecutorService.class)
            .annotatedWith(Names.named("timer"))
            .toInstance(new ManagedScheduledExecutorService(new ScheduledThreadPoolExecutor(1)));
        bind(ScheduledExecutorService.class)
            .annotatedWith(Names.named("notifier"))
            .toInstance(new ManagedScheduledExecutorService(new ScheduledThreadPoolExecutor(1)));
        bind(PluginManager.class).to(DefaultPluginManager.class).asEagerSingleton();
        bind(TagService.class).to(TagServiceImpl.class);
        bind(FileService.class).to(FileServiceImpl.class);
        bind(ConfigService.class).to(ConfigServiceImpl.class);
        bind(ServiceResourceService.class).to(ServiceResourceServiceImpl.class);
        bind(ServiceTemplateService.class).to(ServiceTemplateServiceImpl.class);
        bind(InfraService.class).to(InfraServiceImpl.class);
        bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
        bind(AppService.class).to(AppServiceImpl.class);
        bind(ReleaseService.class).to(ReleaseServiceImpl.class);
        bind(CatalogService.class).to(CatalogServiceImpl.class);
        bind(HostService.class).to(HostServiceImpl.class);
        bind(new TypeLiteral<AbstractQueueListener<CollectEvent>>() {}).to(ArtifactCollectEventListener.class);
        bind(new TypeLiteral<Queue<CollectEvent>>() {}).toInstance(new MongoQueueImpl<>(CollectEvent.class, datastore));
        bind(ArtifactCollectorService.class)
            .annotatedWith(Names.named(SourceType.JENKINS.name()))
            .to(JenkinsArtifactCollectorServiceImpl.class);
        install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
        bind(JenkinsBuildService.class).to(JenkinsBuildServiceImpl.class);
      }
    });
    registerListeners(annotations.stream().filter(annotation -> Listeners.class.isInstance(annotation)).findFirst());
    registerScheduledJobs(injector);
    WingsBootstrap.initialize(injector);
  }

  private void registerListeners(java.util.Optional<Annotation> listenerOptional) {
    if (listenerOptional.isPresent()) {
      for (Class<? extends AbstractQueueListener> queueListenerClass : ((Listeners) listenerOptional.get()).value()) {
        injector.getInstance(QueueListenerController.class).register(injector.getInstance(queueListenerClass), 1);
      }
    }
  }

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
