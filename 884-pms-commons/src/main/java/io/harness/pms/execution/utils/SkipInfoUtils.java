package io.harness.pms.execution.utils;

import io.harness.beans.ParameterField;
import io.harness.data.structure.EmptyPredicate;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SkipInfoUtils {
  public String getSkipCondition(ParameterField<String> skipCondition) {
    if (skipCondition == null) {
      return null;
    }
    if (EmptyPredicate.isNotEmpty(skipCondition.getValue())) {
      return skipCondition.getValue();
    }
    return skipCondition.getExpressionValue();
  }
}
