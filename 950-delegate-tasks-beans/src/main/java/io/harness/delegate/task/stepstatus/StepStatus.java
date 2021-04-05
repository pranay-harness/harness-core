package io.harness.delegate.task.stepstatus;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CI)
public class StepStatus {
  private int numberOfRetries;
  private long totalTimeTakenInMillis;
  private StepExecutionStatus stepExecutionStatus;
  @Builder.Default private StepOutput output = StepMapOutput.builder().build();
  @Builder.Default private String error = "";
}
