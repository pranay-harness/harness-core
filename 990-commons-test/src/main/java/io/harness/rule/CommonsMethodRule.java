package io.harness.rule;

import com.google.inject.Module;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CommonsMethodRule implements MethodRule, InjectorRuleMixin {
  @Override
  public List<Module> modules(List<Annotation> annotations) {
    return new ArrayList<>();
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
