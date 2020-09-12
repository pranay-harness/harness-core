package io.harness.delegate.task.stepstatus;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

@Data
@Builder
public class StepStatus {
  private int numberOfRetries;
  private Duration totalTimeTaken;
  private StepExecutionStatus stepExecutionStatus;
  @Builder.Default private StepOutput output = StepMapOutput.builder().build();
  @Builder.Default private String error = "";
}
