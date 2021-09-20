package io.harness.yaml.core.failurestrategy;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
@RecasterAlias("io.harness.yaml.core.failurestrategy.OnFailureConfig")
public class OnFailureConfig {
  @NotNull List<NGFailureType> errors;
  @NotNull FailureStrategyActionConfig action;
}
