package io.harness.executionplan.rule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.CIBeansModule;
import io.harness.CIExecutionServiceModule;
import io.harness.CIExecutionTestModule;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Initiates mongo connection and register classes for running UTs
 */

public class CIExecutionRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;
  @Rule public CIExecutionTestModule testRule = new CIExecutionTestModule();
  public CIExecutionRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));
    modules.add(new CIExecutionTestModule());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
      }
    });
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
    modules.add(new CIExecutionPersistenceTestModule());
    modules.add(new CIExecutionServiceModule());
    modules.add(new CIBeansModule());
    modules.add(new ExecutionPlanModule());
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
