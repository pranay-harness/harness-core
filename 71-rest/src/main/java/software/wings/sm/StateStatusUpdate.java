package software.wings.sm;

import io.harness.beans.ExecutionStatus;

public interface StateStatusUpdate {
  void stateExecutionStatusUpdated(
      String appId, String workflowExecution, String stateExecutionInstanceId, ExecutionStatus status);
}
