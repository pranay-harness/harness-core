package io.harness.rule;

import static io.harness.AuthorizationServiceHeader.CV_NEXT_GEN;
import static io.harness.CvNextGenTestBase.getResourceFilePath;
import static io.harness.cache.CacheBackend.CAFFEINE;
import static io.harness.cache.CacheBackend.NOOP;

import io.harness.AccessControlClientConfiguration;
import io.harness.AccessControlClientModule;
import io.harness.cache.CacheConfig;
import io.harness.cache.CacheConfig.CacheConfigBuilder;
import io.harness.cache.CacheModule;
import io.harness.cf.AbstractCfModule;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.cvng.CVNGTestConstants;
import io.harness.cvng.CVNextGenCommonsServiceModule;
import io.harness.cvng.CVServiceModule;
import io.harness.cvng.EventsFrameworkModule;
import io.harness.cvng.VerificationConfiguration;
import io.harness.cvng.client.MockedNextGenService;
import io.harness.cvng.client.MockedVerificationManagerService;
import io.harness.cvng.client.NextGenClientModule;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerClientModule;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.impl.AlwaysFalseFeatureFlagServiceImpl;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.ff.FeatureFlagConfig;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.lock.PersistentLockModule;
import io.harness.metrics.modules.MetricsModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.notification.MongoBackendConfiguration;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.notification.constant.NotificationClientSecrets;
import io.harness.notification.module.NotificationClientModule;
import io.harness.notification.module.NotificationClientPersistenceModule;
import io.harness.persistence.HPersistence;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.serializer.CvNextGenRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import java.io.Closeable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.converters.TypeConverter;

@Slf4j
public class CvNextGenRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public CvNextGenRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(CvNextGenRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CvNextGenRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(CvNextGenRegistrars.morphiaConverters)
            .build();
      }
    });

    modules.add(mongoTypeModule(annotations));
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });
    modules.add(new CVNextGenCommonsServiceModule());

    modules.add(TestMongoModule.getInstance());
    VerificationConfiguration verificationConfiguration = getVerificationConfiguration();
    modules.add(Modules.override(new CVServiceModule(verificationConfiguration)).with(binder -> {
      binder.bind(FeatureFlagService.class).to(AlwaysFalseFeatureFlagServiceImpl.class);
      binder.bind(VerificationManagerService.class).to(MockedVerificationManagerService.class);
      binder.bind(Clock.class).toInstance(CVNGTestConstants.FIXED_TIME_FOR_TESTS);
      binder.bind(NextGenService.class).to(MockedNextGenService.class);
    }));
    MongoBackendConfiguration mongoBackendConfiguration =
        MongoBackendConfiguration.builder().uri("mongodb://localhost:27017/notificationChannel").build();
    modules.add(new EventsFrameworkModule(verificationConfiguration.getEventsFrameworkConfiguration()));
    mongoBackendConfiguration.setType("MONGO");
    modules.add(new NotificationClientModule(
        NotificationClientConfiguration.builder()
            .notificationClientBackendConfiguration(mongoBackendConfiguration)
            .serviceHttpClientConfig(ServiceHttpClientConfig.builder()
                                         .baseUrl("http://localhost:9005")
                                         .connectTimeOutSeconds(15)
                                         .readTimeOutSeconds(15)
                                         .build())
            .notificationSecrets(
                NotificationClientSecrets.builder()
                    .notificationClientSecret(
                        "IC04LYMBf1lDP5oeY4hupxd4HJhLmN6azUku3xEbeE3SUx5G3ZYzhbiwVtK4i7AmqyU9OZkwB4v8E9qM")
                    .build())
            .build()));
    modules.add(new NotificationClientPersistenceModule());
    modules.add(new NextGenClientModule(
        NGManagerServiceConfig.builder().managerServiceSecret("secret").ngManagerUrl("http://test-ng-host").build()));
    modules.add(new VerificationManagerClientModule("http://test-host"));
    modules.add(new MetricsModule());
    modules.add(new PersistentLockModule());

    CacheConfigBuilder cacheConfigBuilder =
        CacheConfig.builder().disabledCaches(new HashSet<>()).cacheNamespace("harness-cache");
    if (annotations.stream().anyMatch(annotation -> annotation instanceof Cache)) {
      cacheConfigBuilder.cacheBackend(CAFFEINE);
    } else {
      cacheConfigBuilder.cacheBackend(NOOP);
    }
    CacheModule cacheModule = new CacheModule(cacheConfigBuilder.build());
    modules.add(cacheModule);
    AccessControlClientConfiguration accessControlClientConfiguration =
        AccessControlClientConfiguration.builder()
            .enableAccessControl(false)
            .accessControlServiceSecret("token")
            .accessControlServiceConfig(ServiceHttpClientConfig.builder()
                                            .baseUrl("http://localhost:9006/api/")
                                            .readTimeOutSeconds(15)
                                            .connectTimeOutSeconds(15)
                                            .build())
            .build();
    modules.add(AccessControlClientModule.getInstance(accessControlClientConfiguration, CV_NEXT_GEN.getServiceId()));

    modules.add(new AbstractCfModule() {
      @Override
      public CfClientConfig cfClientConfig() {
        return CfClientConfig.builder().build();
      }

      @Override
      public CfMigrationConfig cfMigrationConfig() {
        return CfMigrationConfig.builder().build();
      }

      @Override
      public FeatureFlagConfig featureFlagConfig() {
        return FeatureFlagConfig.builder().build();
      }
    });
    return modules;
  }

  private VerificationConfiguration getVerificationConfiguration()
      throws java.io.IOException, io.dropwizard.configuration.ConfigurationException {
    final ObjectMapper objectMapper = Jackson.newObjectMapper();
    final Validator validator = Validators.newValidator();
    final YamlConfigurationFactory<VerificationConfiguration> factory =
        new YamlConfigurationFactory<>(VerificationConfiguration.class, validator, objectMapper, "dw");

    // TODO: once we move completely on bazel, we can get rid of mvn way
    final File yaml = new File(getResourceFilePath("test-config.yml"));
    return factory.build(yaml);
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
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(log, statement, frameworkMethod, target);
  }
}
