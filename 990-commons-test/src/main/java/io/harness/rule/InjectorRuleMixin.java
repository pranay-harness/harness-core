/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.lang.annotation.Annotation;
import java.util.List;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface InjectorRuleMixin {
  List<Module> modules(List<Annotation> annotations) throws Exception;

  default void initialize(Injector injector, List<Module> modules) {}

  default Statement applyInjector(Logger log, Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        final List<Annotation> annotations = asList(frameworkMethod.getAnnotations());
        final List<Module> modules = modules(annotations);

        long start = currentTimeMillis();

        Injector injector = Guice.createInjector(modules);

        long created = currentTimeMillis();
        log.info("Creating guice injector took: {}ms", created - start);

        initialize(injector, modules);
        injector.injectMembers(target);

        long initialized = currentTimeMillis();
        log.info("Initializing test injections took: {}ms", initialized - created);

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
