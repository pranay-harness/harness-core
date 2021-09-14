/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ExpressionResolveFunctor {
  String processString(String expression);

  default ResolveObjectResponse processObject(Object o) {
    return new ResolveObjectResponse(false, null);
  }

  default boolean supportsNotExpression() {
    return true;
  }
}
