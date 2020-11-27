package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnTimeoutConfig {
  FailureStrategyActionConfig action;
}
