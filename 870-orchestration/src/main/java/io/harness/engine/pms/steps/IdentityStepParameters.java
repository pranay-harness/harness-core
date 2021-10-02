package io.harness.engine.pms.steps;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IdentityStepParameters implements StepParameters {
  String originalNodeExecutionId;
}
