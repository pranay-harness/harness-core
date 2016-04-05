/**
 *
 */
package software.wings.app;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import software.wings.beans.ReadPref;
import software.wings.dl.MongoConfig;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.*;
import software.wings.service.intfc.*;

import javax.inject.Named;
import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class WingsModule extends AbstractModule {
  private MainConfiguration configuration;

  private Datastore primaryDatastore;

  private Datastore secondaryDatastore;

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

    if (hosts.size() > 1) {
      mongoClient = new MongoClient(serverAddresses);
      mongoClient.setReadPreference(ReadPreference.secondaryPreferred());
      this.secondaryDatastore = m.createDatastore(mongoClient, mongoConfig.getDb());
    } else {
      this.secondaryDatastore = primaryDatastore;
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
    bind(InfraService.class).to(InfraServiceImpl.class);
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
  }
}
