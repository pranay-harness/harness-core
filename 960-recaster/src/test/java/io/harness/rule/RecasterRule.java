package io.harness.rule;

import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;

import com.google.inject.Injector;
import com.google.inject.Module;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

public class RecasterRule implements MethodRule, InjectorRuleMixin {
  ClosingFactory closingFactory;

  public RecasterRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));

    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {}

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
