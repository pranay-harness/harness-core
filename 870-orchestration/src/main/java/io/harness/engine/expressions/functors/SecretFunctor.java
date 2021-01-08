package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

import lombok.Value;

@OwnedBy(CDC)
@Value
public class SecretFunctor implements ExpressionFunctor {
  int expressionFunctorToken;

  public SecretFunctor(int expressionFunctorToken) {
    this.expressionFunctorToken = expressionFunctorToken;
  }

  public Object getValue(String secretIdentifier) {
    return "${ngSecretManager.obtain(\"" + secretIdentifier + "\", " + expressionFunctorToken + ")}";
  }
}
