package io.harness.expression.app;

import io.harness.expression.EngineExpressionEvaluator;

import com.google.inject.AbstractModule;

public class ExpressionServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(EngineExpressionEvaluator.class).toInstance(new EngineExpressionEvaluator(null));
  }
}
