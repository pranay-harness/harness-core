package io.harness.yaml.core.failurestrategy.rollback;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepGroupFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = STEP_GROUP_ROLLBACK)
  NGFailureActionType type = NGFailureActionType.STEP_GROUP_ROLLBACK;
}
