package io.harness.yaml.core.failurestrategy.marksuccess;

import static io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants.MARK_AS_SUCCESS;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarkAsSuccessFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = MARK_AS_SUCCESS) NGFailureActionType type = NGFailureActionType.MARK_AS_SUCCESS;
}
