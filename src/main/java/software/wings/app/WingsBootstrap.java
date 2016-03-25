package software.wings.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import io.dropwizard.setup.Environment;

/**
 *  This class is used initialize all the resources such as Mongo DB Connection Pool, Service registry etc.
 *
 *
 * @author Rishi
 *
 */
public class WingsBootstrap {
  public static <T> T lookup(Class<T> cls) {
    return guiceInjector.getInstance(cls);
  }

  public static void initialize(MainConfiguration configuration, Environment environment) {
    //		config = configuration;
    //		logger.info("Portal URL : " + config.getPortal().getUrl());
    //		MongoConnectionFactory mongoConnectionFactory = configuration.getMongoConnectionFactory();
    //
    //		mongoConnectionFactory.initialize(environment);
    //		MongoClient mongoClient = configuration.getMongoConnectionFactory().getMongoClient();
    //		Datastore datastore = configuration.getMongoConnectionFactory().getDatastore();
    //
    //		environment.lifecycle().manage(new Managed() {
    //			@Override
    //			public void start() {
    //			}
    //
    //			@Override
    //			public void stop() {
    //				if (mongoClient != null) {
    //					mongoClient.close();
    //				}
    //			}
    //		});
    //		environment.healthChecks().register("mongo", new MongoConnectionHealth(mongoClient));

    // initialize factories
    //		WingsMongoPersistence wingsPersistence = new WingsMongoPersistence(datastore);
    //		WaitNotifyEngine.init(wingsPersistence);
    //		StateMachineExecutor.init(wingsPersistence);
    //
    //
    //		//service registry
    //		register(Datastore.class, datastore);
    //		register(AppService.class, new AppServiceImpl(datastore));
    //		register(AuditService.class, new AuditServiceImpl(datastore, mongoClient, mongoConnectionFactory.getDb(),
    //"audits")); 		register(ReleaseService.class, new ReleaseServiceImpl(datastore));
    //		register(ArtifactService.class, new ArtifactServiceImpl(datastore));
    //		register(FileService.class, new FileServiceImpl(mongoClient, mongoConnectionFactory.getDb(), "lob"));
    //		register(NodeSetExecutorService.class, new NodeSetExecutorServiceImpl());
    //		register(SSHNodeSetExecutorService.class, new SSHNodeSetExecutorServiceImpl(datastore, 50)); //TODO -
    //initialize from config 		register(DeploymentService.class, new DeploymentServiceImpl(datastore));
    //		register(InfraService.class, new InfraServiceImpl(datastore));
    //		register(PlatformService.class, new PlatformServiceImpl(datastore));
    //		register(UserService.class, new UserService(datastore));
    //		register(GenericDBCache.class, new GenericDBCache());
  }

  public static MainConfiguration getConfig() {
    return lookup(MainConfiguration.class);
  }

  private static final Logger logger = LoggerFactory.getLogger(WingsBootstrap.class);

  /**
   * @param injector
   */
  public static void initialize(Injector injector) {
    guiceInjector = injector;
  }

  private static Injector guiceInjector;
}
