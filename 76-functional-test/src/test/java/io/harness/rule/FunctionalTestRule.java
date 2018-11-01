package io.harness.rule;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import io.dropwizard.Configuration;
import io.harness.exception.WingsException;
import io.harness.factory.ClosingFactory;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import io.harness.mongo.QueryFactory;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.ExecutorModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.service.impl.EventEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class FunctionalTestRule implements MethodRule, MongoRuleMixin, DistributedLockRuleMixin {
  private static final Logger logger = LoggerFactory.getLogger(FunctionalTestRule.class);

  private Injector injector;
  private ClosingFactory closingFactory = new ClosingFactory();
  private int port;
  protected AdvancedDatastore datastore;
  private ExecutorService executorService = new CurrentThreadExecutor();

  String PORTAL_URL = "PORTAL_URL";

  String VERIFICATION_PATH = "VERIFICATION_PATH";

  protected void before() {
    List<Module> modules = new ArrayList<>();
    MongoClient mongoClient;
    String dbName = System.getProperty("dbName", "harness");
    try {
      MongoClientURI clientUri =
          new MongoClientURI(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName), mongoClientOptions);
      if (!clientUri.getURI().startsWith("mongodb://localhost:")) {
        // Protection against running tests on non-local databases such as prod or qa.
        // Comment out this throw exception if you're sure.
        throw new WingsException("\n*** WARNING *** : Attempting to run test on non-local Mongo: " + clientUri.getURI()
            + "\n*** Exiting *** : Comment out this check in WingsRule.java "
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
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, dbName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoClient, dbName, closingFactory);

    Configuration configuration = getConfiguration(dbName);

    modules = getRequiredModules(configuration, distributedLockSvc);
    modules.add(new QueueModule(false));
    injector = Guice.createInjector(modules);
  }

  protected Configuration getConfiguration(String dbName) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setAllowedDomains("wings.software");
    configuration.getPortal().setUrl(PORTAL_URL);
    configuration.getPortal().setVerificationUrl(VERIFICATION_PATH);
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    return configuration;
  }

  protected void after() {
    closingFactory.stopServers();
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
    modules.add(new TemplateModule());
    return modules;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        before();
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          after();
        }
      }
    };
  }
}
