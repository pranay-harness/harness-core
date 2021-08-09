package io.harness.pms.sdk.core.supporter.async;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestStepParameters implements StepParameters {
  String param;
}
