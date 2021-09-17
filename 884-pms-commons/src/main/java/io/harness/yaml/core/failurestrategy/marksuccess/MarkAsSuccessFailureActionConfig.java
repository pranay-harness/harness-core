package io.harness.yaml.core.failurestrategy.marksuccess;

import static io.harness.beans.rollback.NGFailureActionTypeConstants.MARK_AS_SUCCESS;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@RecasterAlias("io.harness.yaml.core.failurestrategy.marksuccess.MarkAsSuccessFailureActionConfig")
public class MarkAsSuccessFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = MARK_AS_SUCCESS) NGFailureActionType type = NGFailureActionType.MARK_AS_SUCCESS;
}
