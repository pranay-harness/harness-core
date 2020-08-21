package io.harness.expression.app;

import com.google.inject.AbstractModule;

import io.harness.expression.evaluator.ExpressionServiceEvaluator;

public class ExpressionServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ExpressionServiceEvaluator.class).toInstance(new ExpressionServiceEvaluator());
  }
}
