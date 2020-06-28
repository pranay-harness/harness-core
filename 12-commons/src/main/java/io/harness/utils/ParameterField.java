package io.harness.utils;

import io.harness.expression.ExpressionEvaluatorUtils;
import lombok.Getter;

@Getter
public class ParameterField<T> {
  private String expressionValue;
  private boolean isExpression;
  private T value;

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

  public Object get(String key) {
    return isExpression ? expressionValue : ExpressionEvaluatorUtils.fetchField(value, key).orElse(null);
  }

  public void updateWithExpression(String newExpression) {
    isExpression = true;
    expressionValue = newExpression;
  }

  public void updateWithValue(Object newValue) {
    isExpression = false;
    value = (T) newValue;
  }
}
