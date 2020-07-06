package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.rule.InjectorRuleMixin;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NextGenRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CoreTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  @Override
  public List<Module> modules(List<Annotation> annotations) {
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
    modules.add(new SecretManagementModule(
        SecretManagerClientConfig.builder().baseUrl("http://localhost:8080/").build(), "test_secret"));
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return NextGenRegistrars.kryoRegistrars;
      }
    });
    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(base, method, target);
  }
}
