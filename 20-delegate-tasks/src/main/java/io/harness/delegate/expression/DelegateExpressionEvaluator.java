package io.harness.delegate.expression;

import io.harness.expression.ExpressionEvaluator;

import java.util.Map;

public class DelegateExpressionEvaluator extends ExpressionEvaluator {
  public DelegateExpressionEvaluator(Map<String, char[]> evaluatedSecrets, int expressionFunctorToken) {
    addFunctor("secretDelegate",
        SecretDelegateFunctor.builder()
            .secrets(evaluatedSecrets)
            .expressionFunctorToken(expressionFunctorToken)
            .build());
  }
}
