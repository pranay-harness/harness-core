package io.harness.rule;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class BypassRule implements TestRule {
  private static final Logger logger = LoggerFactory.getLogger(OwnerRule.class);

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Bypass {}

  @Override
  public Statement apply(Statement statement, Description description) {
    Bypass bypass = description.getAnnotation(Bypass.class);
    if (bypass == null) {
      return statement;
    }
    return RepeatRule.RepeatStatement.builder().build();
  }
  }
