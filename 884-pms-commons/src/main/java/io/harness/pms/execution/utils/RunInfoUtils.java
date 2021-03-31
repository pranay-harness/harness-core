package io.harness.pms.execution.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.ParameterField;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class RunInfoUtils {
  public String getRunCondition(ParameterField<String> whenCondition, boolean isStage) {
    if (ParameterField.isNull(whenCondition)) {
      return getDefaultWhenCondition(isStage);
    }
    return getGivenRunCondition(whenCondition);
  }

  public String getRunConditionForRollback(ParameterField<String> whenCondition) {
    if (ParameterField.isNull(whenCondition)) {
      return "<+OnStageFailure>";
    }
    return getGivenRunCondition(whenCondition);
  }

  private String getGivenRunCondition(ParameterField<String> whenCondition) {
    if (EmptyPredicate.isNotEmpty(whenCondition.getValue())) {
      return whenCondition.getValue();
    }
    return whenCondition.getExpressionValue();
  }

  private String getDefaultWhenCondition(boolean isStage) {
    if (!isStage) {
      return "<+OnStageSuccess>";
    }
    return "<+OnPipelineSuccess>";
  }
}
