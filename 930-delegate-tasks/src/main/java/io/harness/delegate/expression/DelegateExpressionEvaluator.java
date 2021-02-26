package io.harness.delegate.expression;

import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ImageSecretFunctor;
import io.harness.expression.JsonFunctor;

import java.util.Map;

public class DelegateExpressionEvaluator extends ExpressionEvaluator {
  public DelegateExpressionEvaluator() {
    addFunctor("json", new JsonFunctor());
  }

  public DelegateExpressionEvaluator(Map<String, char[]> evaluatedSecrets, int expressionFunctorToken) {
    addFunctor("secretDelegate",
        SecretDelegateFunctor.builder()
            .secrets(evaluatedSecrets)
            .expressionFunctorToken(expressionFunctorToken)
            .build());
    addFunctor(ImageSecretFunctor.FUNCTOR_NAME, new ImageSecretFunctor());
  }
}
