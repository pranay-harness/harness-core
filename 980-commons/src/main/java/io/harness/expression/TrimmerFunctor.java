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
