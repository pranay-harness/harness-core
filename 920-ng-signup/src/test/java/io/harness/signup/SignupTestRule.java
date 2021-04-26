package io.harness.signup;

import static io.harness.lock.DistributedLockImplementation.NOOP;
import static io.harness.mongo.MongoModule.defaultMongoClientOptions;

import static org.mockito.Mockito.mock;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.AbstractCfModule;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.ng.core.services.OrganizationService;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.redis.RedisConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.RbacCoreRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.lang.Nullable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
public class SignupTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());
    List<Module> modules = new ArrayList<>();

    MongoClientURI clientUri =
        new MongoClientURI("mongodb://localhost:7457", MongoClientOptions.builder(defaultMongoClientOptions));
    String dbName = clientUri.getDatabase();

    MongoClient mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:7457"));

    modules.add(new SignupModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret", "Service"));

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(RbacCoreRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Named("primaryDatastore")
      @Singleton
      AdvancedDatastore datastore(Morphia morphia) {
        return (AdvancedDatastore) morphia.createDatastore(mongoClient, "dbName");
      }

      @Provides
      @Singleton
      @Nullable
      UserProvider userProvider() {
        return new NoopUserProvider();
      }

      @Provides
      @Named("lock")
      @Singleton
      RedisConfig redisLockConfig() {
        return RedisConfig.builder().build();
      }

      @Provides
      @Singleton
      DistributedLockImplementation distributedLockImplementation() {
        return NOOP;
      }

      @Provides
      @Named("locksMongoClient")
      @Singleton
      public MongoClient locksMongoClient(ClosingFactory closingFactory) {
        return mongoClient;
      }

      @Provides
      @Named("locksDatabase")
      @Singleton
      String databaseNameProvider() {
        return dbName;
      }
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        MapBinder.newMapBinder(binder(), String.class, Migrator.class);
        bind(OrganizationService.class).toInstance(mock(OrganizationService.class));
        bind(AccountService.class).toInstance(mock(AccountService.class));
        bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
      }
    });
    modules.add(new AbstractCfModule() {
      @Override
      public CfClientConfig cfClientConfig() {
        return CfClientConfig.builder().build();
      }

      @Override
      public CfMigrationConfig cfMigrationConfig() {
        return CfMigrationConfig.builder().build();
      }
    });

    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(log, base, method, target);
  }
}
