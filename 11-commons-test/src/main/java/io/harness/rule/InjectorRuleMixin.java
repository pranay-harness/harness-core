package io.harness.rule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.junit.runners.model.Statement;

import java.util.List;

public interface InjectorRuleMixin {
  List<Module> modules();

  default void initialize(Injector injector) {}

  default Statement applyInjector(Statement statement, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Injector injector = Guice.createInjector(modules());
        initialize(injector);
        injector.injectMembers(target);
        statement.evaluate();
      }
    };
  }
}
