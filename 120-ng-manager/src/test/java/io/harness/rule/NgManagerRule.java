package io.harness.rule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.entityreferenceclient.EntityReferenceClientModule;
import io.harness.entityreferenceclient.NGManagerClientConfig;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.CoreModule;
import io.harness.ng.core.CorePersistenceTestModule;
import io.harness.ng.core.SecretManagementModule;
import io.harness.ng.remote.client.ServiceHttpClientConfig;
import io.harness.persistence.HPersistence;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class NgManagerRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public NgManagerRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });
    modules.add(mongoTypeModule(annotations));
    modules.add(new CoreModule());
    modules.add(new CorePersistenceTestModule());
    modules.add(KryoModule.getInstance());
    modules.add(new SecretManagementModule());
    modules.add(new SecretManagementClientModule(
        ServiceHttpClientConfig.builder().baseUrl("http://localhost:8080/").build(), "test_secret"));
    modules.add(new EntityReferenceClientModule(
        NGManagerClientConfig.builder().baseUrl("http://localhost:7457/").build(), "test_secret", "ng-manager"));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return NextGenRegistrars.kryoRegistrars;
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return NextGenRegistrars.morphiaRegistrars;
      }
    });
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
