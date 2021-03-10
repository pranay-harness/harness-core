package io.harness.yaml.core.failurestrategy.rollback;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.STAGE_ROLLBACK;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StageRollbackFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = STAGE_ROLLBACK) NGFailureActionType type = NGFailureActionType.STAGE_ROLLBACK;
}
