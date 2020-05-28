package io.harness.utils;

import lombok.Getter;

public class ParameterField<T> {
  @Getter private final String expressionValue;
  @Getter private final boolean isExpression;
  @Getter private final T value;

  private static final ParameterField<?> EMPTY = new ParameterField<>(null, false, null);

  public static <T> ParameterField<T> createField(T value, boolean isExpression, String expressionValue) {
    return new ParameterField<>(value, isExpression, expressionValue);
  }

  private ParameterField(T value, boolean isExpression, String expressionValue) {
    this.value = value;
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
  }
  public static <T> ParameterField<T> ofNull() {
    return (ParameterField<T>) EMPTY;
  }
}
