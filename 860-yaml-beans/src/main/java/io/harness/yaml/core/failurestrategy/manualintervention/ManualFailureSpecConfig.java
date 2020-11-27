package io.harness.yaml.core.failurestrategy.manualintervention;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualFailureSpecConfig {
  @NotNull String timeout;
  @NotNull OnTimeoutConfig onTimeout;
}
