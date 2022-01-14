package io.harness.engine.pms.steps.identity;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._860_ORCHESTRATION_STEPS)
@OwnedBy(HarnessTeam.PIPELINE)
public class IdentityStepParameters implements StepParameters {
  String originalNodeExecutionId;
}
