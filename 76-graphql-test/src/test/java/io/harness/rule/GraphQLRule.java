package io.harness.rule;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.hazelcast.core.HazelcastInstance;
import graphql.GraphQL;
import io.harness.event.EventsModule;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.factory.ClosingFactory;
import io.harness.module.TestMongoModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.QueryFactory;
import io.harness.persistence.HPersistence;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.BroadcasterFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CacheModule;
import software.wings.app.GraphQLModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.graphql.provider.QueryLanguageProvider;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.security.authentication.oauth.AzureConfig;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.GithubConfig;
import software.wings.security.authentication.oauth.GitlabConfig;
import software.wings.security.authentication.oauth.GoogleConfig;
import software.wings.security.authentication.oauth.LinkedinConfig;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class GraphQLRule
    implements MethodRule, InjectorRuleMixin, MongoRuleMixin, DistributedLockRuleMixin, BypassRuleMixin {
  ClosingFactory closingFactory;
  @Getter private AdvancedDatastore datastore;
  @Getter private GraphQL graphQL;

  public GraphQLRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public void initialize(Injector injector) {
    final QueryLanguageProvider<GraphQL> instance =
        injector.getInstance(Key.get(new TypeLiteral<QueryLanguageProvider<GraphQL>>() {}));
    graphQL = instance.getQL();
    injector.getInstance(HPersistence.class).registerUserProvider(new ThreadLocalUserProvider());
  }

  protected MainConfiguration getConfiguration(String dbName) {
    MainConfiguration configuration = new MainConfiguration();
    configuration.getPortal().setCompanyName("COMPANY_NAME");
    configuration.getPortal().setAllowedDomains("wings.software");
    configuration.getPortal().setUrl("PORTAL_URL");
    configuration.getPortal().setVerificationUrl("VERIFICATION_PATH");
    configuration.getPortal().setJwtExternalServiceSecret("JWT_EXTERNAL_SERVICE_SECRET");
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getBackgroundSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    configuration.getServiceSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));

    MarketoConfig marketoConfig =
        MarketoConfig.builder().clientId("client_id").clientSecret("client_secret_id").enabled(false).build();

    AzureConfig azureConfig =
        AzureConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    LinkedinConfig linkedinConfig =
        LinkedinConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GoogleConfig googleConfig =
        GoogleConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GitlabConfig gitlabConfig =
        GitlabConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    GithubConfig githubConfig =
        GithubConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();
    BitbucketConfig bitbucketConfig =
        BitbucketConfig.builder().clientId("clientId").clientSecret("secret").callbackUrl("callbackUrl").build();

    configuration.setMarketoConfig(marketoConfig);
    configuration.setAzureConfig(azureConfig);
    configuration.setLinkedinConfig(linkedinConfig);
    configuration.setGoogleConfig(googleConfig);
    configuration.setBitbucketConfig(bitbucketConfig);
    configuration.setGithubConfig(githubConfig);
    configuration.setGitlabConfig(gitlabConfig);
    return configuration;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    String databaseName = databaseName();
    MongoInfo mongoInfo = testMongo(annotations, closingFactory);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    datastore = (AdvancedDatastore) morphia.createDatastore(mongoInfo.getClient(), databaseName);
    datastore.setQueryFactory(new QueryFactory());

    DistributedLockSvc distributedLockSvc = distributedLockSvc(mongoInfo.getClient(), databaseName, closingFactory);

    List<Module> modules = new ArrayList();
    modules.add(VersionModule.getInstance());
    modules.addAll(TimeModule.getInstance().cumulativeDependencies());
    modules.add(new TestMongoModule(datastore, datastore, distributedLockSvc));

    MainConfiguration configuration = getConfiguration("graphQL");

    CacheModule cacheModule = new CacheModule(configuration);
    modules.add(cacheModule);

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HazelcastInstance.class).toInstance(cacheModule.getHazelcastInstance());
        bind(BroadcasterFactory.class).toInstance(mock(BroadcasterFactory.class));
      }
    });

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerQueueModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule(configuration));
    modules.add(new GraphQLModule());

    return modules;
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    if (bypass(frameworkMethod)) {
      return noopStatement();
    }

    return applyInjector(statement, frameworkMethod, target);
  }
}
