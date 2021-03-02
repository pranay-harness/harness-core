package io.harness.pms.yaml;

import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import io.harness.pms.serializer.json.JsonOrchestrationIgnore;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParameterField<T> implements VisitorFieldWrapper {
  public static final VisitorFieldType VISITOR_FIELD_TYPE = VisitorFieldType.builder().type("PARAMETER_FIELD").build();

  @NotExpression private String expressionValue;
  private boolean expression;
  private T value;
  private boolean typeString;

  // This field is set when runtime input with validation is given.
  private InputSetValidator inputSetValidator;

  // Below 2 fields are when caller wants to set String field instead of T like for some errors, input set merge, etc.
  private boolean jsonResponseField;
  private String responseField;

  private static final ParameterField<?> EMPTY = new ParameterField<>(null, false, null, false, null, false, null);

  public static <T> ParameterField<T> createExpressionField(
      boolean isExpression, String expressionValue, InputSetValidator inputSetValidator, boolean isTypeString) {
    return new ParameterField<>(null, isExpression, expressionValue, inputSetValidator, isTypeString);
  }

  public static <T> ParameterField<T> createValueField(T value) {
    return new ParameterField<>(value, false, null, null, value.getClass().equals(String.class));
  }

  public static <T> ParameterField<T> createValueFieldWithInputSetValidator(
      T value, InputSetValidator inputSetValidator, boolean isTypeString) {
    return new ParameterField<>(value, false, null, inputSetValidator, isTypeString);
  }

  public static <T> ParameterField<T> createJsonResponseField(String responseField) {
    return new ParameterField<>(true, responseField);
  }

  public static <T> ParameterField<T> ofNull() {
    return (ParameterField<T>) EMPTY;
  }

  @Builder
  public ParameterField(String expressionValue, boolean expression, T value, boolean typeString,
      InputSetValidator inputSetValidator, boolean jsonResponseField, String responseField) {
    this.expressionValue = expressionValue;
    this.expression = expression;
    this.value = value;
    this.typeString = typeString;
    this.inputSetValidator = inputSetValidator;
    this.jsonResponseField = jsonResponseField;
    this.responseField = responseField;
  }

  public ParameterField(
      T value, boolean expression, String expressionValue, InputSetValidator inputSetValidator, boolean typeString) {
    this(expressionValue, expression, value, typeString, inputSetValidator, false, null);
  }

  private ParameterField(boolean jsonResponseField, String responseField) {
    this(null, false, null, false, null, jsonResponseField, responseField);
  }

  public Object get(String key) {
    return expression ? expressionValue : ExpressionEvaluatorUtils.fetchField(value, key).orElse(null);
  }

  public void updateWithExpression(String newExpression) {
    expression = true;
    expressionValue = newExpression;
  }

  public void updateWithValue(Object newValue) {
    expression = false;
    value = (T) newValue;
  }

  @JsonIgnore
  @JsonOrchestrationIgnore
  public Object getJsonFieldValue() {
    if (expression) {
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
      return jsonResponseField ? responseField : value;
    }
  }

  public Object fetchFinalValue() {
    return expression ? expressionValue : value;
  }

  @Override
  @JsonIgnore
  @JsonOrchestrationIgnore
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
