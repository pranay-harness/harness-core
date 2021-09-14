/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

import lombok.Value;

@OwnedBy(CDC)
@Value
public class SecretFunctor implements ExpressionFunctor {
  long expressionFunctorToken;

  public SecretFunctor(long expressionFunctorToken) {
    this.expressionFunctorToken = expressionFunctorToken;
  }

  public Object getValue(String secretIdentifier) {
    return "${ngSecretManager.obtain(\"" + secretIdentifier + "\", " + expressionFunctorToken + ")}";
  }
}
