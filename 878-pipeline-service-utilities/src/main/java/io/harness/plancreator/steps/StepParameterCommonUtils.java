package io.harness.plancreator.steps;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.TimeoutUtils;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class StepParameterCommonUtils {
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

  public StepElementParametersBuilder getStepParameters(AbstractStepNode stepElementConfig) {
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

  public StepElementParametersBuilder getStepParameters(
          AbstractStepNode stepElementConfig, OnFailRollbackParameters failRollbackParameters) {
    StepElementParametersBuilder stepBuilder = getStepParameters(stepElementConfig);
    stepBuilder.rollbackParameters(failRollbackParameters);
    return stepBuilder;
  }
}
