package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class OnTimeoutConfig {
  FailureStrategyActionConfig action;
}
