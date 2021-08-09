package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class FailureStrategyConfig {
  @NotNull OnFailureConfig onFailure;
}
