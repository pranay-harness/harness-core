package software.wings.beans;

import lombok.Builder;
import lombok.Value;
import software.wings.sm.ExecutionStatus;

import java.util.List;

@Builder
@Value
public class StateExecutionElement {
  private String executionContextElementId;
  private ExecutionStatus status;
  private String name;
  private int progress;
  private List<String> runningSteps;
}
