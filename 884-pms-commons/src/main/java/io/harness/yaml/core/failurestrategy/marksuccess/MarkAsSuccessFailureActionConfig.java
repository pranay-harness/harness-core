package io.harness.yaml.core.failurestrategy.marksuccess;

import static io.harness.beans.rollback.NGFailureActionTypeConstants.MARK_AS_SUCCESS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class MarkAsSuccessFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = MARK_AS_SUCCESS) NGFailureActionType type = NGFailureActionType.MARK_AS_SUCCESS;
}
