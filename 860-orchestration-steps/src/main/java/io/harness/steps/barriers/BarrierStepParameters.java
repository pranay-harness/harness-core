package io.harness.steps.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("barrierStepParameters")
public class BarrierStepParameters implements StepParameters {
  String identifier;
}
