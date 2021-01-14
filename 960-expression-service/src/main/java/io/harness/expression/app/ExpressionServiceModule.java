package io.harness.expression.app;

import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.evaluator.ExpressionServiceEvaluator;

import com.google.inject.AbstractModule;

public class ExpressionServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(EngineExpressionEvaluator.class).toInstance(new EngineExpressionEvaluator(null));
  }
}
