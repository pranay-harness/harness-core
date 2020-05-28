package io.harness.rule;

import static java.util.Arrays.asList;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.List;

public interface InjectorRuleMixin {
  List<Module> modules(List<Annotation> annotations) throws Exception;

  default void initialize(Injector injector, List<Module> modules) {}

  default Statement applyInjector(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final List<Annotation> annotations = asList(frameworkMethod.getAnnotations());
        final List<Module> modules = modules(annotations);

        Injector injector = Guice.createInjector(modules);
        initialize(injector, modules);

        injector.injectMembers(target);
        try {
          statement.evaluate();
        } catch (RuntimeException exception) {
          LoggerFactory.getLogger(InjectorRuleMixin.class).error("Test exception", exception);
          throw exception;
        }
      }
    };
  }
}
