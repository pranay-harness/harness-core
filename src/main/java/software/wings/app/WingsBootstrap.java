package software.wings.app;

import java.util.HashMap;
import java.util.Map;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import software.wings.dl.GenericDBCache;
import software.wings.dl.MongoConnectionFactory;
import software.wings.health.MongoConnectionHealth;
import software.wings.service.UserService;
import software.wings.service.impl.AppServiceImpl;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.impl.DeploymentServiceImpl;
import software.wings.service.impl.FileServiceImpl;
import software.wings.service.impl.AuditServiceImpl;
import software.wings.service.impl.InfraServiceImpl;
import software.wings.service.impl.NodeSetExecutorServiceImpl;
import software.wings.service.impl.PlatformServiceImpl;
import software.wings.service.impl.ReleaseServiceImpl;
import software.wings.service.impl.SSHNodeSetExecutorServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DeploymentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.NodeSetExecutorService;
import software.wings.service.intfc.PlatformService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.SSHNodeSetExecutorService;

/**
 *  This class is used initialize all the resources such as Mongo DB Connection Pool, Service registry etc.
 *
 *
 * @author Rishi
 *
 */
public class WingsBootstrap {
  private static Map<String, Object> instanceMap = new HashMap<>();
  private static MainConfiguration config;
  private static <T> void register(Class<T> cls, T t) {
    instanceMap.put(cls.getName(), t);
  }

  public static <T> T lookup(Class<T> cls) {
    return (T) instanceMap.get(cls.getName());
  }

  public static void initialize(MainConfiguration configuration, Environment environment) {
    config = configuration;
    logger.info("Portal URL : " + config.getPortal().getUrl());
    MongoConnectionFactory mongoConnectionFactory = configuration.getMongoConnectionFactory();

    mongoConnectionFactory.initialize(environment);
    MongoClient mongoClient = configuration.getMongoConnectionFactory().getMongoClient();
    Datastore datastore = configuration.getMongoConnectionFactory().getDatastore();

    environment.lifecycle().manage(new Managed() {
      @Override
      public void start() {}

      @Override
      public void stop() {
        if (mongoClient != null) {
          mongoClient.close();
        }
      }
    });
    environment.healthChecks().register("mongo", new MongoConnectionHealth(mongoClient));

    // service registry
    register(Datastore.class, datastore);
    register(AppService.class, new AppServiceImpl(datastore));
    register(
        AuditService.class, new AuditServiceImpl(datastore, mongoClient, mongoConnectionFactory.getDb(), "audits"));
    register(ReleaseService.class, new ReleaseServiceImpl(datastore));
    register(ArtifactService.class, new ArtifactServiceImpl(datastore));
    register(FileService.class, new FileServiceImpl(mongoClient, mongoConnectionFactory.getDb(), "lob"));
    register(NodeSetExecutorService.class, new NodeSetExecutorServiceImpl());
    register(SSHNodeSetExecutorService.class,
        new SSHNodeSetExecutorServiceImpl(datastore, 50)); // TODO - initialize from config
    register(DeploymentService.class, new DeploymentServiceImpl(datastore));
    register(InfraService.class, new InfraServiceImpl(datastore));
    register(PlatformService.class, new PlatformServiceImpl(datastore));
    register(UserService.class, new UserService(datastore));
    register(GenericDBCache.class, new GenericDBCache());
  }

  public static MainConfiguration getConfig() {
    return config;
  }

  private static final Logger logger = LoggerFactory.getLogger(WingsBootstrap.class);
}
