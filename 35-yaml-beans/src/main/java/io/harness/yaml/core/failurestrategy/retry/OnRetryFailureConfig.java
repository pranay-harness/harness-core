package io.harness.yaml.core.failurestrategy.retry;

import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnRetryFailureConfig {
  FailureStrategyActionConfig action;
}
