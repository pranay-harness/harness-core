package io.harness.yaml.core.failurestrategy.ignore;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IgnoreFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.IGNORE;
}
