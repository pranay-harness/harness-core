package io.harness.yaml.core.failurestrategy.rollback;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StepGroupFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.STEP_GROUP_ROLLBACK;
}
