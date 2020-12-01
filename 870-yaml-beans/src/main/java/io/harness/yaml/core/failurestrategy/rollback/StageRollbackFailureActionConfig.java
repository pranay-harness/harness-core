package io.harness.yaml.core.failurestrategy.rollback;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.STAGE_ROLLBACK)
public class StageRollbackFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.STAGE_ROLLBACK;
}
