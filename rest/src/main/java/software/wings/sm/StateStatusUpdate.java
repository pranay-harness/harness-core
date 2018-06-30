package software.wings.sm;

public interface StateStatusUpdate {
  void stateExecutionStatusUpdated(String workflowExecution, String stateExecutionInstanceId, ExecutionStatus status);
}
