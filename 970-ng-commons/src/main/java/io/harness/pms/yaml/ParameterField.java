/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.yaml;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.expression.NotExpression;
import io.harness.pms.yaml.validation.InputSetValidator;
import io.harness.utils.YamlPipelineUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.IOException;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@OwnedBy(PIPELINE)
@RecasterAlias("parameterField")
@Slf4j
public class ParameterField<T> {
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
    return new ParameterField<>(value, false, null, null, value != null && value.getClass().equals(String.class));
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
    value = null;
  }

  public void updateWithValue(Object newValue) {
    expression = false;
    expressionValue = null;
    value = (T) newValue;
  }

  @JsonIgnore
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

  public static boolean isNull(ParameterField<?> actualField) {
    if (actualField == null) {
      return true;
    }

    if (ofNull().equals(actualField)) {
      return true;
    }

    if (actualField.getExpressionValue() != null || actualField.getInputSetValidator() != null
        || actualField.getResponseField() != null || actualField.getValue() != null) {
      return false;
    }
    // Every flag should be false.
    return !actualField.isExpression() && !actualField.isJsonResponseField() && !actualField.isTypeString();
  }

  public static boolean containsInputSetValidator(String value) {
    try {
      ParameterField<?> parameterField = YamlPipelineUtils.read(value, ParameterField.class);
      return parameterField.getInputSetValidator() != null;
    } catch (IOException e) {
      throw new InvalidRequestException(value + " is not a valid value for runtime input");
    }
  }

  public static String getValueFromParameterFieldWithInputSetValidator(String value) {
    try {
      ParameterField<?> parameterField = YamlPipelineUtils.read(value, ParameterField.class);
      if (parameterField.getInputSetValidator() != null) {
        return parameterField.getValue().toString();
      }
      log.error("getValueFromParameterFieldWithInputSetValidator was called for value [" + value
          + "] that does not have an input set validator");
      return null;
    } catch (IOException e) {
      throw new InvalidRequestException(value + " is not a valid value for runtime input");
    }
  }
}
