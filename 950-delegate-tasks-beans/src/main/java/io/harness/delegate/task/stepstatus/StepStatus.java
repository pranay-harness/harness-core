package io.harness.delegate.task.stepstatus;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepStatus {
  private int numberOfRetries;
  private Duration totalTimeTaken;
  private StepExecutionStatus stepExecutionStatus;
  @Builder.Default private StepOutput output = StepMapOutput.builder().build();
  @Builder.Default private String error = "";
}
