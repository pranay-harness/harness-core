package io.harness.utils;

import io.harness.expression.EngineExpressionValue;
import io.harness.expression.ExpressionEvaluatorUtils;
import lombok.Getter;

public class ParameterField<T> implements EngineExpressionValue {
  @Getter private final String expressionValue;
  @Getter private final boolean isExpression;
  @Getter private final T value;

  private static final ParameterField<?> EMPTY = new ParameterField<>(null, false, null);

  public static <T> ParameterField<T> createField(T value, boolean isExpression, String expressionValue) {
    return new ParameterField<>(value, isExpression, expressionValue);
  }

  public static <T> ParameterField<T> createField(T value) {
    return new ParameterField<>(value, false, null);
  }

  private ParameterField(T value, boolean isExpression, String expressionValue) {
    this.value = value;
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
  }
  public static <T> ParameterField<T> ofNull() {
    return (ParameterField<T>) EMPTY;
  }

  @Override
  public Object fetchConcreteValue() {
    return isExpression ? expressionValue : value;
  }

  public Object get(String key) {
    return isExpression ? expressionValue : ExpressionEvaluatorUtils.fetchField(value, key).orElse(null);
  }
}
