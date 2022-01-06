/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import lombok.Getter;

@Getter
public class DummyField<T> {
  private String expressionValue;
  private boolean isExpression;
  private T value;

  public DummyField(boolean isExpression, String expressionValue, T value) {
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
    this.value = value;
  }

  public static <T> DummyField<T> createExpressionField(String expressionValue) {
    return new DummyField<>(true, expressionValue, null);
  }

  public static <T> DummyField<T> createValueField(T value) {
    return new DummyField<>(false, null, value);
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

  public boolean process(DummyFunctor functor) {
    Object newValue;
    boolean updated = true;
    if (isExpression) {
      newValue = functor.evaluateExpression(expressionValue);
      if (newValue instanceof String && functor.hasVariables((String) newValue)) {
        String newExpression = (String) newValue;
        if (newExpression.equals(expressionValue)) {
          return false;
        }

        updateWithExpression(newExpression);
        return true;
      }

      updateWithValue(newValue);
    } else {
      updated = false;
      newValue = value;
    }

    if (newValue != null) {
      Object finalValue = ExpressionEvaluatorUtils.updateExpressions(newValue, functor);
      if (finalValue != null) {
        updateWithValue(finalValue);
        updated = true;
      }
    }

    return updated;
  }
}
