package io.harness.signup;

import static org.mockito.Mockito.mock;

import io.harness.account.services.AccountService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoPersistence;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.ng.core.services.OrganizationService;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.RbacCoreRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
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
    });

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        MapBinder.newMapBinder(binder(), String.class, Migrator.class);
        bind(OrganizationService.class).toInstance(mock(OrganizationService.class));
        bind(AccountService.class).toInstance(mock(AccountService.class));
      }
    });

    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(log, base, method, target);
  }
}
