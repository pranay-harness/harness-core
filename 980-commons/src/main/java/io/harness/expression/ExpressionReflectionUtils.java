/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import io.harness.expression.Expression.SecretsMode;
import io.harness.reflection.ReflectionUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExpressionReflectionUtils {
  public interface Functor {
    String update(SecretsMode mode, String value);
  }

  public interface NestedAnnotationResolver {}

  public static void applyExpression(Object object, Functor functor) {
    ReflectionUtils.<Expression>updateAnnotatedField(Expression.class, object,
        (expression, value) -> functor.update(SecretsMode.valueOf(expression.value()), value));
  }
}
