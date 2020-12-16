package io.harness.rule;

import io.harness.cvng.CVNextGenCommonsServiceModule;
import io.harness.cvng.CVServiceModule;
import io.harness.cvng.client.NextGenClientModule;
import io.harness.cvng.client.VerificationManagerClientModule;
import io.harness.cvng.core.NGManagerServiceConfig;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.notification.MongoBackendConfiguration;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.notification.constant.NotificationClientSecrets;
import io.harness.notification.module.NotificationClientModule;
import io.harness.persistence.HPersistence;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.serializer.CvNextGenRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    modules.add(new CVServiceModule());
    MongoBackendConfiguration mongoBackendConfiguration =
        MongoBackendConfiguration.builder().uri("mongodb://localhost:27017/notificationChannel").build();
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
    modules.add(new NextGenClientModule(
        NGManagerServiceConfig.builder().managerServiceSecret("secret").ngManagerUrl("http://test-ng-host").build()));
    modules.add(new VerificationManagerClientModule("http://test-host"));
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
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
