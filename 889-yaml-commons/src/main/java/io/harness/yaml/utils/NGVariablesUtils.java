/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class NGVariablesUtils {
  public Map<String, Object> getMapOfVariables(List<NGVariable> variables, long expressionFunctorToken) {
    Map<String, Object> mapOfVariables = new HashMap<>();
    if (EmptyPredicate.isEmpty(variables)) {
      return mapOfVariables;
    }
    for (NGVariable variable : variables) {
      if (variable instanceof SecretNGVariable) {
        SecretNGVariable secretNGVariable = (SecretNGVariable) variable;
        String secretValue = getSecretValue(secretNGVariable);
        if (secretValue != null) {
          String value = "${ngSecretManager.obtain(\"" + secretValue + "\", " + expressionFunctorToken + ")}";
          mapOfVariables.put(variable.getName(), value);
        }
      } else {
        ParameterField<?> value = getNonSecretValue(variable);
        if (value != null) {
          mapOfVariables.put(variable.getName(), value);
        }
      }
    }
    return mapOfVariables;
  }

  public Map<String, Object> getMapOfVariables(List<NGVariable> variables) {
    Map<String, Object> mapOfVariables = new HashMap<>();
    if (EmptyPredicate.isEmpty(variables)) {
      return mapOfVariables;
    }
    for (NGVariable variable : variables) {
      if (variable instanceof SecretNGVariable) {
        SecretNGVariable secretNGVariable = (SecretNGVariable) variable;
        String secretValue = getSecretValue(secretNGVariable);
        if (secretValue != null) {
          String value = "<+secrets.getValue(\"" + secretValue + "\")>";
          mapOfVariables.put(variable.getName(), value);
        }
      } else {
        ParameterField<?> value = getNonSecretValue(variable);
        if (value != null) {
          mapOfVariables.put(variable.getName(), value);
        }
      }
    }
    return mapOfVariables;
  }

  private String getSecretValue(SecretNGVariable variable) {
    ParameterField<SecretRefData> value = (ParameterField<SecretRefData>) variable.getCurrentValue();
    if (ParameterField.isNull(value)
        || (!value.isExpression() && (value.getValue() == null || value.getValue().isNull()))) {
      if (variable.isRequired()) {
        throw new InvalidRequestException(
            String.format("Value not provided for required secret variable: %s", variable.getName()));
      }
      return null;
    }
    return value.isExpression() ? value.getExpressionValue() : value.getValue().toSecretRefStringValue();
  }

  private ParameterField<?> getNonSecretValue(NGVariable variable) {
    ParameterField<?> value = variable.getCurrentValue();
    if (ParameterField.isNull(value) || (!value.isExpression() && value.getValue() == null)) {
      if (variable.isRequired()) {
        throw new InvalidRequestException(
            String.format("Value not provided for required variable: %s", variable.getName()));
      }
      return null;
    }
    return value;
  }

  public Map<String, Object> applyVariableOverrides(
      Map<String, Object> originalVariablesMap, List<NGVariable> overrideVariables, long expressionFunctorToken) {
    if (EmptyPredicate.isEmpty(overrideVariables)) {
      return originalVariablesMap;
    }

    Map<String, Object> overrideVariablesMap = getMapOfVariables(overrideVariables, expressionFunctorToken);
    originalVariablesMap.putAll(overrideVariablesMap);
    return originalVariablesMap;
  }
}
