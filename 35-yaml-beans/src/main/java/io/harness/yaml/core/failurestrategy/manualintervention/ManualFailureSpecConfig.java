package io.harness.yaml.core.failurestrategy.manualintervention;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class ManualFailureSpecConfig {
  @NotNull String timeout;
  @NotNull OnTimeoutConfig onTimeout;
}
