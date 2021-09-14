/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.core.failurestrategy.rollback;

import static io.harness.beans.rollback.NGFailureActionTypeConstants.STAGE_ROLLBACK;

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
public class StageRollbackFailureActionConfig implements FailureStrategyActionConfig {
  @ApiModelProperty(allowableValues = STAGE_ROLLBACK) NGFailureActionType type = NGFailureActionType.STAGE_ROLLBACK;
}
