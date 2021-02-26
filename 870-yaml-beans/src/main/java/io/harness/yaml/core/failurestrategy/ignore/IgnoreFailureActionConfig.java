package io.harness.yaml.core.failurestrategy.ignore;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.IGNORE;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IgnoreFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = IGNORE) NGFailureActionType type = NGFailureActionType.IGNORE;
}
