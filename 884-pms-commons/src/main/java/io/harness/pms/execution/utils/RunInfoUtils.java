package io.harness.pms.execution.utils;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RunInfoUtils {
  public String getRunCondition(ParameterField<String> whenCondition) {
    if (whenCondition == null) {
      return null;
    }
    if (hasSome(whenCondition.getValue())) {
      return whenCondition.getValue();
    }
    return whenCondition.getExpressionValue();
  }
}
