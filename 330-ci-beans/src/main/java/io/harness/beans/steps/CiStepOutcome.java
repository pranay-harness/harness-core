package io.harness.beans.steps;

import io.harness.data.Outcome;
import io.harness.delegate.task.stepstatus.StepOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CiStepOutcome implements Outcome {
  StepOutput output;
}
