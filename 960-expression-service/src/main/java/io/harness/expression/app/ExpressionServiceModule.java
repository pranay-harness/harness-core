package io.harness.expression.app;

import io.harness.expression.evaluator.ExpressionServiceEvaluator;

import com.google.inject.AbstractModule;

public class ExpressionServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ExpressionServiceEvaluator.class).toInstance(new ExpressionServiceEvaluator());
  }
}
