/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
