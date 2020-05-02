package io.harness.rule;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import graphql.GraphQL;
import io.dropwizard.Configuration;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.configuration.ConfigurationType;
import io.harness.e2e.AbstractE2ETest;
import io.harness.event.EventsModule;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ServersModule;
import io.harness.lock.mongo.MongoPersistentLocker;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.rest.RestResponse;
import io.harness.scm.ScmSecret;
import io.harness.security.AsymmetricDecryptor;
import io.harness.testframework.framework.ManagerExecutor;
import io.harness.testframework.framework.Setup;
import io.harness.testframework.framework.constants.FrameworkConstants;
import io.harness.testframework.framework.utils.TestUtils;
import io.harness.testlib.module.TestMongoDatabaseName;
import io.harness.testlib.module.TestMongoModule;
import io.harness.testlib.rule.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import lombok.Getter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.AuthModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GraphQLModule;
import software.wings.app.LicenseModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.SSOModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.EventEmitter;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.GenericType;

public class LocalPortalTestRule implements MethodRule, MongoRuleMixin, InjectorRuleMixin {
  private int port;
  ClosingFactory closingFactory;

  public LocalPortalTestRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  protected AdvancedDatastore datastore;
  private ExecutorService executorService = new CurrentThreadExecutor();
  private static final String alpnJar =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  @Getter private GraphQL graphQL;

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    String alpn = "/home/jenkins/maven-repositories/0/";
    ManagerExecutor.ensureManager(AbstractE2ETest.class, alpn, alpnJar);
    RestResponse<MongoConfig> mongoConfigRestResponse =
        Setup.portal()
            .queryParam("configurationType", ConfigurationType.MONGO)
            .get("/health/configuration")
            .as(new GenericType<RestResponse<MongoConfig>>() {}.getType());

    String mongoUri =
        new AsymmetricDecryptor(new ScmSecret()).decryptText(mongoConfigRestResponse.getResource().getEncryptedUri());

    MongoClientURI clientUri = new MongoClientURI(mongoUri, mongoClientOptions);
    String dbName = clientUri.getDatabase();

    MongoClient mongoClient = new MongoClient(clientUri);
    closingFactory.addServer(mongoClient);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, dbName);
    datastore.setQueryFactory(new QueryFactory());

    Configuration configuration = getConfiguration(mongoUri);

    return getRequiredModules(configuration, mongoClient, dbName);
  }

  protected Configuration getConfiguration(String mongoUri) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setUrl("PORTAL_URL");
    configuration.getPortal().setVerificationUrl("VERIFICATION_PATH");
    configuration.setMongoConnectionFactory(MongoConfig.builder().uri(mongoUri).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    return configuration;
  }

  protected List<Module> getRequiredModules(
      Configuration configuration, MongoClient locksMongoClient, String locksDatabase) {
    io.harness.threading.ExecutorModule.getInstance().setExecutorService(executorService);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList<>();
    modules.add(new TestMongoDatabaseName(locksDatabase));

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EventEmitter.class).toInstance(mock(EventEmitter.class));
        bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
        bind(MetricRegistry.class);
        bind(CommandLibraryServiceHttpClient.class).toInstance(mock(CommandLibraryServiceHttpClient.class));
      }
    });
    modules.add(new LicenseModule());
    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new TestMongoModule(datastore, locksMongoClient).cumulativeDependencies());
    modules.addAll(new WingsModule((MainConfiguration) configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule((MainConfiguration) configuration));
    modules.add(new GraphQLModule());
    modules.add(new SSOModule());
    modules.add(new AuthModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }

    final QueryLanguageProvider<GraphQL> instance =
        injector.getInstance(Key.get(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}));
    graphQL = instance.getPrivateGraphQL();

    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    if (!TestUtils.getExecutionEnvironment().equals(FrameworkConstants.LOCAL_ENV)) {
      return statement;
    }
    return applyInjector(statement, frameworkMethod, target);
  }

  @Override
  public void destroy(Injector injector, List<Module> modules) throws Exception {
    injector.getInstance(MongoPersistentLocker.class).stop();
  }
}
