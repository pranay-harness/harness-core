package io.harness.yaml.core.failurestrategy.abort;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.ABORT;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AbortFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = ABORT) NGFailureActionType type = NGFailureActionType.ABORT;
}
