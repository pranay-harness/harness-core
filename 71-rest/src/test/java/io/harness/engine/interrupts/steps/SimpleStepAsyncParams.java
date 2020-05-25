package io.harness.engine.interrupts.steps;

import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SimpleStepAsyncParams implements StepParameters {
  boolean shouldFail;
  boolean shouldThrowException;
  int duration;
}
