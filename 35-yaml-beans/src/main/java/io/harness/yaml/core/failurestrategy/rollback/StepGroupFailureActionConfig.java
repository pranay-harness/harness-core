package io.harness.yaml.core.failurestrategy.rollback;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureActionTypeConstants;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName(NGFailureActionTypeConstants.STEP_GROUP_ROLLBACK)
public class StepGroupFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.STEP_GROUP_ROLLBACK;
}
