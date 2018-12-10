package io.harness.expression;

import io.harness.reflection.ReflectionUtils;

public class ExpressionReflectionUtils {
  public static void applyExpression(Object o, ReflectionUtils.Functor functor) {
    ReflectionUtils.updateFieldValues(o, f -> f.isAnnotationPresent(Expression.class), functor);
  }
}
