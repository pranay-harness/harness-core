package software.wings.sm;

public interface StateStatusUpdate {
  void stateExecutionStatusUpdated(
      String appId, String workflowExecution, String stateExecutionInstanceId, ExecutionStatus status);
}
