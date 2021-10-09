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
