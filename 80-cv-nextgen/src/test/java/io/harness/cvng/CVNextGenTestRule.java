package io.harness.cvng;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

import io.harness.app.CVServiceModule;
import io.harness.app.cvng.client.VerificationManagerClientModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CVNextGenTestRule implements InjectorRuleMixin, MethodRule, MongoRuleMixin {
  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());
    List<Module> modules = new ArrayList<>();
    modules.add(mongoTypeModule(annotations));
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });
    modules.add(new CVNextGenCommonsServiceModule());
    modules.addAll(new TestMongoModule().cumulativeDependencies());
    modules.add(new CVServiceModule());
    modules.add(new VerificationManagerClientModule("http://test-host"));
    return modules;
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    return applyInjector(base, method, target);
  }
}
