package io.harness.yaml.core.failurestrategy.abort;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AbortFailureActionConfig implements FailureStrategyActionConfig {
  NGFailureActionType type = NGFailureActionType.ABORT;
}
