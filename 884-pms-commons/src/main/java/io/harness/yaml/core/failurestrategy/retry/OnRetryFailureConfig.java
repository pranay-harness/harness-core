package io.harness.yaml.core.failurestrategy.retry;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class OnRetryFailureConfig {
  FailureStrategyActionConfig action;
}
