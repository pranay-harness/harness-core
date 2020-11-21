package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@OwnedBy(CDC)
@Value
public class SecretFunctor {
  int expressionFunctorToken;

  public SecretFunctor(int expressionFunctorToken) {
    this.expressionFunctorToken = expressionFunctorToken;
  }

  public Object getValue(String secretName) {
    return "${secretManager.obtain(\"" + secretName + "\", " + expressionFunctorToken + ")}";
  }
}
