package io.harness.expression.field.dummy;

import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.pms.expression.OrchestrationField;
import io.harness.pms.expression.OrchestrationFieldType;

import lombok.Getter;

@Getter
public class DummyOrchestrationField<T> implements OrchestrationField {
  public static final OrchestrationFieldType ORCHESTRATION_FIELD_TYPE =
      OrchestrationFieldType.builder().type("DUMMY").build();

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

  @Override
  public Class<? extends OrchestrationField> getDeserializationClass() {
    return DummyOrchestrationField.class;
  }

  @Override
  public OrchestrationFieldType getType() {
    return ORCHESTRATION_FIELD_TYPE;
  }

  @Override
  public Object fetchFinalValue() {
    return isExpression ? expressionValue : value;
  }
}
