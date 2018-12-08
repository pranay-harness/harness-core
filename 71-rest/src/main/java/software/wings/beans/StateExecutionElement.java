package software.wings.beans;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StateExecutionElement {
  private String executionContextElementId;
  private ExecutionStatus status;
  private String name;
  private int progress;
  private List<String> runningSteps;
}
