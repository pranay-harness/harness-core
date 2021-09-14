/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.app;

import io.harness.expression.EngineExpressionEvaluator;

import com.google.inject.AbstractModule;

public class ExpressionServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(EngineExpressionEvaluator.class).toInstance(new EngineExpressionEvaluator(null));
  }
}
