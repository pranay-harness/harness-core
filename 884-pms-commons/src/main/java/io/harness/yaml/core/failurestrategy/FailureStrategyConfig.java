package io.harness.yaml.core.failurestrategy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._889_YAML_COMMONS)
public class FailureStrategyConfig {
  @NotNull OnFailureConfig onFailure;
}
