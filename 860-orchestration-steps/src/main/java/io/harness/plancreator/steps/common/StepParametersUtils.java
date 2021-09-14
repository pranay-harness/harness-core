/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.ParameterFieldHelper;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.utils.NGVariablesUtils;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class StepParametersUtils {
  public StepElementParametersBuilder getStepParameters(StepElementConfig stepElementConfig) {
    StepElementParametersBuilder stepBuilder = StepElementParameters.builder();
    stepBuilder.name(stepElementConfig.getName());
    stepBuilder.identifier(stepElementConfig.getIdentifier());
    stepBuilder.delegateSelectors(stepElementConfig.getDelegateSelectors());
    stepBuilder.description(stepElementConfig.getDescription());
    stepBuilder.failureStrategies(stepElementConfig.getFailureStrategies());
    stepBuilder.skipCondition(stepElementConfig.getSkipCondition());
    stepBuilder.timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepElementConfig.getTimeout())));
    stepBuilder.when(stepElementConfig.getWhen());
    stepBuilder.type(stepElementConfig.getType());
    stepBuilder.uuid(stepElementConfig.getUuid());

    return stepBuilder;
  }

  public StepElementParametersBuilder getStepParameters(
      StepElementConfig stepElementConfig, OnFailRollbackParameters failRollbackParameters) {
    StepElementParametersBuilder stepBuilder = getStepParameters(stepElementConfig);
    stepBuilder.rollbackParameters(failRollbackParameters);
    return stepBuilder;
  }

  public StageElementParametersBuilder getStageParameters(StageElementConfig stageElementConfig) {
    TagUtils.removeUuidFromTags(stageElementConfig.getTags());

    StageElementParametersBuilder stageBuilder = StageElementParameters.builder();
    stageBuilder.name(stageElementConfig.getName());
    stageBuilder.identifier(stageElementConfig.getIdentifier());
    stageBuilder.description(
        ParameterFieldHelper.getParameterFieldHandleValueNull(stageElementConfig.getDescription()));
    stageBuilder.failureStrategies(stageElementConfig.getFailureStrategies());
    stageBuilder.skipCondition(stageElementConfig.getSkipCondition());
    stageBuilder.when(stageElementConfig.getWhen());
    stageBuilder.type(stageElementConfig.getType());
    stageBuilder.uuid(stageElementConfig.getUuid());
    stageBuilder.variables(
        ParameterField.createValueField(NGVariablesUtils.getMapOfVariables(stageElementConfig.getVariables())));
    stageBuilder.tags(stageElementConfig.getTags());

    return stageBuilder;
  }
}
