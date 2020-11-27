package io.harness.yaml.core.failurestrategy;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FailureStrategyConfig {
  @NotNull OnFailureConfig onFailure;
}
