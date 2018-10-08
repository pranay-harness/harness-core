package io.harness.rule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.factory.ClosingFactory;
import org.junit.runners.model.Statement;

import java.util.List;

public interface InjectorRuleMixin {
  List<Module> modules(ClosingFactory closingFactory);

  default Statement applyInjector(Statement statement, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ClosingFactory closingFactory = new ClosingFactory();
        Injector injector = Guice.createInjector(modules(closingFactory));
        injector.injectMembers(target);
        try {
          statement.evaluate();
        } finally {
          closingFactory.stopServers();
        }
      }
    };
  }
}
