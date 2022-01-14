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
