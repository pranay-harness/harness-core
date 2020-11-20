package io.harness.beans;

import com.google.common.base.Suppliers;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

import io.harness.CIBeansModule;
import io.harness.callback.DelegateCallbackToken;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ServersModule;
import io.harness.rule.InjectorRuleMixin;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CIBeansRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  private final ClosingFactory closingFactory;

  public CIBeansRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(new TypeLiteral<Supplier<DelegateCallbackToken>>() {})
            .toInstance(Suppliers.ofInstance(DelegateCallbackToken.newBuilder().build()));
      }
    });
    modules.add(CIBeansModule.getInstance());
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
