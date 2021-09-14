/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public class TrimmerFunctor implements ExpressionResolveFunctor {
  @Override
  public String processString(String expression) {
    return expression == null ? null : expression.trim();
  }

  @Override
  public boolean supportsNotExpression() {
    return false;
  }
}
