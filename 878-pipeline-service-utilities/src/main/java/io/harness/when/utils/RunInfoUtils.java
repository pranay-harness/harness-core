/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.when.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.when.beans.StageWhenCondition;
import io.harness.when.beans.StepWhenCondition;
import io.harness.when.beans.WhenConditionStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class RunInfoUtils {
  String STAGE_SUCCESS = "OnStageSuccess";
  String STAGE_FAILURE = "OnStageFailure";
  String PIPELINE_SUCCESS = "OnPipelineSuccess";
  String PIPELINE_FAILURE = "OnPipelineFailure";
  String ALWAYS = "Always";

  public String getRunCondition(StageWhenCondition stageWhenCondition) {
    if (stageWhenCondition == null) {
      return getDefaultWhenCondition(true);
    }
    if (stageWhenCondition.getPipelineStatus() == null) {
      throw new InvalidRequestException("Pipeline Status in stage when condition cannot be empty.");
    }

    return combineExpressions(getStatusExpression(stageWhenCondition.getPipelineStatus(), true),
        getGivenRunCondition(stageWhenCondition.getCondition()));
  }

  public String getRunCondition(StepWhenCondition stepWhenCondition) {
    if (stepWhenCondition == null) {
      return getDefaultWhenCondition(false);
    }
    if (stepWhenCondition.getStageStatus() == null) {
      throw new InvalidRequestException("Stage Status in step when condition cannot be empty.");
    }

    return combineExpressions(getStatusExpression(stepWhenCondition.getStageStatus(), false),
        getGivenRunCondition(stepWhenCondition.getCondition()));
  }

  public String getRunConditionForRollback(StepWhenCondition stepWhenCondition) {
    if (stepWhenCondition == null) {
      return getStatusExpression(STAGE_FAILURE);
    }
    if (stepWhenCondition.getStageStatus() == null) {
      throw new InvalidRequestException("Stage Status in step when condition cannot be empty.");
    }

    return combineExpressions(getStatusExpression(stepWhenCondition.getStageStatus(), false),
        getGivenRunCondition(stepWhenCondition.getCondition()));
  }

  private String getGivenRunCondition(ParameterField<String> condition) {
    if (EmptyPredicate.isNotEmpty(condition.getValue())) {
      return condition.getValue();
    }
    return condition.getExpressionValue();
  }

  private String getDefaultWhenCondition(boolean isStage) {
    if (!isStage) {
      return getStatusExpression(STAGE_SUCCESS);
    }
    return getStatusExpression(PIPELINE_SUCCESS);
  }

  private String combineExpressions(String statusExpression, String conditionExpression) {
    if (EmptyPredicate.isNotEmpty(conditionExpression)) {
      return statusExpression + " && (" + conditionExpression + ")";
    }
    return statusExpression;
  }

  private String getStatusExpression(WhenConditionStatus whenConditionStatus, boolean isStage) {
    switch (whenConditionStatus) {
      case SUCCESS:
        return isStage ? getStatusExpression(PIPELINE_SUCCESS) : getStatusExpression(STAGE_SUCCESS);
      case FAILURE:
        return isStage ? getStatusExpression(PIPELINE_FAILURE) : getStatusExpression(STAGE_FAILURE);
      default:
        return getStatusExpression(ALWAYS);
    }
  }

  private String getStatusExpression(String status) {
    return "<+" + status + ">";
  }
}
