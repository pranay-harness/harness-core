package io.harness.pms.execution.utils;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RunInfoUtils {
  public String getRunCondition(ParameterField<String> whenCondition, boolean isStage) {
    if (whenCondition == null) {
      return getDefaultWhenCondition(isStage);
    }
    if (EmptyPredicate.isNotEmpty(whenCondition.getValue())) {
      return whenCondition.getValue();
    }
    return whenCondition.getExpressionValue();
  }

  private String getDefaultWhenCondition(boolean isStage) {
    if (isStage) {
      return "<+<+stage.currentStatus> == \"SUCCEEDED\">";
    }
    return "<+<+pipeline.currentStatus> == \"SUCCEEDED\">";
  }
}
