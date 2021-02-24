package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.yaml.core.timeout.Timeout;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualFailureSpecConfig {
  @NotNull Timeout timeout;
  @NotNull OnTimeoutConfig onTimeout;
}
