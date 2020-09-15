package io.harness.utils;

import com.fasterxml.jackson.annotation.JsonValue;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// public class ParameterField<T> implements OrchestrationField {
public class ParameterField<T> {
  //  public static final OrchestrationFieldType ORCHESTRATION_FIELD_TYPE =
  //      OrchestrationFieldType.builder().type("PARAMETER_FIELD").build();

  @NotExpression private String expressionValue;
  private boolean isExpression;
  private T value;
  // This field is set when runtime input with validation is given.
  private InputSetValidator inputSetValidator;

  // Below 2 fields are when caller wants to set String field instead of T like for some errors, input set merge, etc.
  private boolean isResponseFieldString;
  private String responseField;

  private static final ParameterField<?> EMPTY = new ParameterField<>(null, false, null, null, false, null);

  public static <T> ParameterField<T> createExpressionField(
      boolean isExpression, String expressionValue, InputSetValidator inputSetValidator) {
    return new ParameterField<>(null, isExpression, expressionValue, inputSetValidator);
  }

  public static <T> ParameterField<T> createValueField(T value) {
    return new ParameterField<>(value, false, null, null);
  }

  public static <T> ParameterField<T> createStringResponseField(String responseField) {
    return new ParameterField<>(true, responseField);
  }

  private ParameterField(T value, boolean isExpression, String expressionValue, InputSetValidator inputSetValidator) {
    this.value = value;
    this.isExpression = isExpression;
    this.expressionValue = expressionValue;
    this.inputSetValidator = inputSetValidator;
  }

  private ParameterField(String expressionValue, boolean isExpression, T value, InputSetValidator inputSetValidator,
      boolean isResponseFieldString, String responseField) {
    this.expressionValue = expressionValue;
    this.isExpression = isExpression;
    this.value = value;
    this.inputSetValidator = inputSetValidator;
    this.isResponseFieldString = isResponseFieldString;
    this.responseField = responseField;
  }

  private ParameterField(boolean isResponseFieldString, String responseField) {
    this.isResponseFieldString = isResponseFieldString;
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

  @JsonValue
  public Object getJsonFieldValue() {
    if (isExpression) {
      StringBuilder result = new StringBuilder(expressionValue);
      if (inputSetValidator != null) {
        result.append(".")
            .append(inputSetValidator.getValidatorType().getYamlName())
            .append("(")
            .append(inputSetValidator.getParameters())
            .append(")");
      }
      return result.toString();
    } else if (isResponseFieldString) {
      return responseField;
    }
    return value;
  }

  //  @Override
  //  public OrchestrationFieldType getType() {
  //    return ORCHESTRATION_FIELD_TYPE;
  //  }
  //
  //  @Override
  //  public Object getFinalValue() {
  //    return isExpression ? expressionValue : value;
  //  }
}
