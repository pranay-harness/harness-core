package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.TimeoutUtils;

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
    StageElementParametersBuilder stageBuilder = StageElementParameters.builder();
    stageBuilder.name(stageElementConfig.getName());
    stageBuilder.identifier(stageElementConfig.getIdentifier());
    stageBuilder.description(stageElementConfig.getDescription());
    stageBuilder.failureStrategies(stageElementConfig.getFailureStrategies());
    stageBuilder.skipCondition(stageElementConfig.getSkipCondition());
    stageBuilder.when(stageElementConfig.getWhen());
    stageBuilder.type(stageElementConfig.getType());
    stageBuilder.uuid(stageElementConfig.getUuid());
    stageBuilder.originalVariables(stageElementConfig.getVariables());

    return stageBuilder;
  }
}
