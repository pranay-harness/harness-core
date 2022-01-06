/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
