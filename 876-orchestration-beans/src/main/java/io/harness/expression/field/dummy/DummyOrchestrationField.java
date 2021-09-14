/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.field.dummy;

import io.harness.expression.ExpressionEvaluatorUtils;

import lombok.Getter;

@Getter
public class DummyOrchestrationField<T> {
  private boolean isExpression;
  private String expressionValue;
  private boolean isString;
  private T value;

  public DummyOrchestrationField(boolean isExpression, String expressionValue, T value) {
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
    this.value = value;
  }

  public static <T> DummyOrchestrationField<T> createExpressionField(String expressionValue) {
    return new DummyOrchestrationField<>(true, expressionValue, null);
  }

  public static <T> DummyOrchestrationField<T> createValueField(T value) {
    return new DummyOrchestrationField<>(false, null, value);
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

  public Object fetchFinalValue() {
    return isExpression ? expressionValue : value;
  }
}
