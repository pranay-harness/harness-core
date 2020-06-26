package io.harness.ng.core;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.PersistenceTestModule;
import io.harness.testlib.module.MongoRuleMixin;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

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
    modules.add(new PersistenceTestModule());
    modules.add(new SecretManagementModule(
        SecretManagerClientConfig.builder().baseUrl("http://localhost:8080/").serviceSecret("test_secret").build()));
    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(base, method, target);
  }
}
