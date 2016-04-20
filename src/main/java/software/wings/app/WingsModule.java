/**
 *
 */
package software.wings.app;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.ifesdjeen.timer.HashedWheelTimer;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import ro.fortsoft.pf4j.DefaultPluginManager;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.beans.ReadPref;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.MongoQueueImpl;
import software.wings.core.queue.Queue;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.service.impl.*;
import software.wings.service.intfc.*;
import software.wings.utils.ManagedExecutorService;
import software.wings.utils.ManagedScheduledExecutorService;
import software.wings.waitNotify.NotifyEvent;
import software.wings.waitNotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static software.wings.common.thread.ThreadPool.create;

/**
 * @author Rishi
 */
public class WingsModule extends AbstractModule {
  private MainConfiguration configuration;

  private Datastore primaryDatastore;

  private Datastore secondaryDatastore;

  private DistributedLockSvc distributedLockSvc;

  private Map<ReadPref, Datastore> datastoreMap = Maps.newHashMap();
  /**
   * @param configuration
   */
  public WingsModule(MainConfiguration configuration) {
    this.configuration = configuration;
    MongoConfig mongoConfig = configuration.getMongoConnectionFactory();
    List<String> hosts = Splitter.on(",").splitToList(mongoConfig.getHost());
    List<ServerAddress> serverAddresses = new ArrayList<>();

    for (String host : hosts) {
      serverAddresses.add(new ServerAddress(host, mongoConfig.getPort()));
    }
    Morphia m = new Morphia();
    MongoClient mongoClient = new MongoClient(serverAddresses);
    this.primaryDatastore = m.createDatastore(mongoClient, mongoConfig.getDb());
    distributedLockSvc = new ManagedDistributedLockSvc(
        new DistributedLockSvcFactory(new DistributedLockSvcOptions(mongoClient, mongoConfig.getDb(), "locks"))
            .getLockSvc());

    if (hosts.size() > 1) {
      mongoClient = new MongoClient(serverAddresses);
      mongoClient.setReadPreference(ReadPreference.secondaryPreferred());
      this.secondaryDatastore = m.createDatastore(mongoClient, mongoConfig.getDb());
    } else {
      this.secondaryDatastore = primaryDatastore;
    }

    m.mapPackage("software.wings.beans");
    this.primaryDatastore.ensureIndexes();
    if (hosts.size() > 1) {
      this.secondaryDatastore.ensureIndexes();
    }

    datastoreMap.put(ReadPref.CRITICAL, primaryDatastore);
    datastoreMap.put(ReadPref.NORMAL, secondaryDatastore);
  }

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(MainConfiguration.class).toInstance(configuration);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(AppService.class).to(AppServiceImpl.class);
    bind(ArtifactService.class).to(ArtifactServiceImpl.class);
    bind(AuditService.class).to(AuditServiceImpl.class);
    bind(DeploymentService.class).to(DeploymentServiceImpl.class);
    bind(FileService.class).to(FileServiceImpl.class);
    bind(NodeSetExecutorService.class).to(NodeSetExecutorServiceImpl.class);
    bind(SSHNodeSetExecutorService.class).to(SSHNodeSetExecutorServiceImpl.class);
    bind(PlatformService.class).to(PlatformServiceImpl.class);
    bind(ReleaseService.class).to(ReleaseServiceImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);
    bind(RoleService.class).to(RoleServiceImpl.class);
    bind(ServiceResourceService.class).to(ServiceResourceServiceImpl.class);
    bind(Datastore.class).annotatedWith(Names.named("primaryDatastore")).toInstance(primaryDatastore);
    bind(Datastore.class).annotatedWith(Names.named("secondaryDatastore")).toInstance(secondaryDatastore);
    bind(new TypeLiteral<Map<ReadPref, Datastore>>() {})
        .annotatedWith(Names.named("datastoreMap"))
        .toInstance(datastoreMap);
    bind(ExecutorService.class).toInstance(new ManagedExecutorService(create(20, 1000, 500L, TimeUnit.MILLISECONDS)));
    bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
    bind(ServiceTemplateService.class).to(ServiceTemplateServiceImpl.class);
    bind(InfraService.class).to(InfraServiceImpl.class);
    bind(WorkflowService.class).to(WorkflowServiceImpl.class);
    bind(PluginManager.class).to(DefaultPluginManager.class).asEagerSingleton();
    bind(DistributedLockSvc.class).toInstance(distributedLockSvc);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("timer"))
        .toInstance(new ManagedScheduledExecutorService(new HashedWheelTimer()));
    bind(new TypeLiteral<Queue<NotifyEvent>>() {})
        .toInstance(new MongoQueueImpl<>(NotifyEvent.class, primaryDatastore));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("notifier"))
        .toInstance(new ManagedScheduledExecutorService(new HashedWheelTimer()));
    bind(new TypeLiteral<AbstractQueueListener<NotifyEvent>>() {}).to(NotifyEventListener.class);
  }
}
