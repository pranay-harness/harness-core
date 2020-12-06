package io.harness.beans;

import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import io.harness.pms.sdk.core.expression.OrchestrationField;
import io.harness.pms.sdk.core.expression.OrchestrationFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParameterField<T> implements OrchestrationField, VisitorFieldWrapper {
  public static final OrchestrationFieldType ORCHESTRATION_FIELD_TYPE =
      OrchestrationFieldType.builder().type("PARAMETER_FIELD").build();

  public static final VisitorFieldType VISITOR_FIELD_TYPE = VisitorFieldType.builder().type("PARAMETER_FIELD").build();

  @NotExpression private String expressionValue;
  private boolean isExpression;
  private T value;
  private boolean isTypeString;

  // This field is set when runtime input with validation is given.
  private InputSetValidator inputSetValidator;

  // Below 2 fields are when caller wants to set String field instead of T like for some errors, input set merge, etc.
  private boolean isJsonResponseField;
  private String responseField;

  private static final ParameterField<?> EMPTY = new ParameterField<>(null, false, null, null, false, null);

  public static <T> ParameterField<T> createExpressionField(
      boolean isExpression, String expressionValue, InputSetValidator inputSetValidator, boolean isTypeString) {
    return new ParameterField<>(null, isExpression, expressionValue, inputSetValidator, isTypeString);
  }

  public static <T> ParameterField<T> createValueField(T value) {
    return new ParameterField<>(value, false, null, null, value.getClass().equals(String.class));
  }

  public static <T> ParameterField<T> createJsonResponseField(String responseField) {
    return new ParameterField<>(true, responseField);
  }

  public ParameterField(T value, boolean isExpression, String expressionValue, InputSetValidator inputSetValidator,
      boolean isTypeString) {
    this.value = value;
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
    this.inputSetValidator = inputSetValidator;
    this.isTypeString = isTypeString;
  }

  private ParameterField(String expressionValue, boolean isExpression, T value, InputSetValidator inputSetValidator,
      boolean isJsonResponseField, String responseField) {
    this.expressionValue = expressionValue;
    this.isExpression = isExpression;
    this.value = value;
    this.inputSetValidator = inputSetValidator;
    this.isJsonResponseField = isJsonResponseField;
    this.responseField = responseField;
  }

  private ParameterField(boolean isJsonResponseField, String responseField) {
    this.isJsonResponseField = isJsonResponseField;
    this.responseField = responseField;
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

  public Object getJsonFieldValue() {
    if (isExpression) {
      StringBuilder result = new StringBuilder(expressionValue);
      if (inputSetValidator != null) {
        result.append('.')
            .append(inputSetValidator.getValidatorType().getYamlName())
            .append('(')
            .append(inputSetValidator.getParameters())
            .append(')');
      }
      return result.toString();
    } else {
      return isJsonResponseField ? responseField : value;
    }
  }

  @Override
  public OrchestrationFieldType getType() {
    return ORCHESTRATION_FIELD_TYPE;
  }

  @Override
  public Object getFinalValue() {
    return isExpression ? expressionValue : value;
  }

  @Override
  public VisitorFieldType getVisitorFieldType() {
    return VISITOR_FIELD_TYPE;
  }

  public static boolean isNull(ParameterField<?> actualField) {
    if (actualField == null) {
      return true;
    }
    if (actualField.getExpressionValue() != null || actualField.getInputSetValidator() != null
        || actualField.getResponseField() != null || actualField.getValue() != null) {
      return false;
    }
    // Every flag should be false.
    return !actualField.isExpression() && !actualField.isJsonResponseField() && !actualField.isTypeString();
  }
}
