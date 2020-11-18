package io.harness.yaml.core.failurestrategy;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class FailureStrategyConfig {
  @NotNull OnFailureConfig onFailure;
}
