package io.harness.rule;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface BypassRuleMixin {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  @interface Bypass {}

  default boolean bypass(FrameworkMethod method) {
    Bypass bypass = method.getAnnotation(Bypass.class);
    return bypass != null;
  }

  class NoopStatement extends Statement {
    @Override
    public void evaluate() throws Throwable {}
  }

  default Statement noopStatement() {
    return new NoopStatement();
  }
  }
