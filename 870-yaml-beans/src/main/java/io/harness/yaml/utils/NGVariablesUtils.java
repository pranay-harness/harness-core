package io.harness.yaml.utils;

import io.harness.data.structure.EmptyPredicate;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGVariablesUtils {
  public Map<String, Object> getMapOfVariables(List<NGVariable> variables, int expressionFunctorToken) {
    Map<String, Object> mapOfVariables = new HashMap<>();
    if (EmptyPredicate.isEmpty(variables)) {
      return mapOfVariables;
    }
    for (NGVariable variable : variables) {
      if (variable instanceof SecretNGVariable) {
        SecretNGVariable secretNGVariable = (SecretNGVariable) variable;
        String secretValue = secretNGVariable.getValue().getValue() != null
            ? secretNGVariable.getValue().getValue().toSecretRefStringValue()
            : secretNGVariable.getValue().getExpressionValue();
        String value = "${ngSecretManager.obtain(\"" + secretValue + "\", " + expressionFunctorToken + ")}";
        mapOfVariables.put(variable.getName(), value);
      } else {
        mapOfVariables.put(variable.getName(), variable.getValue());
      }
    }
    return mapOfVariables;
  }

  public Map<String, Object> applyVariableOverrides(
      Map<String, Object> originalVariablesMap, List<NGVariable> overrideVariables) {
    if (EmptyPredicate.isEmpty(overrideVariables)) {
      return originalVariablesMap;
    }
    overrideVariables.forEach(
        overrideVariable -> originalVariablesMap.put(overrideVariable.getName(), overrideVariable.getValue()));
    return originalVariablesMap;
  }
}
