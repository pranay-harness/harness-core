package io.harness.engine.interrupts.steps;

import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleStepAsyncParams implements StepParameters {
  boolean shouldFail;
  boolean shouldThrowException;
  int duration;
}
