package software.wings.sm.status;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Value;
import software.wings.sm.StateExecutionInstance;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class StateStatusUpdateInfo {
  @NotNull String appId;
  @NotNull String workflowExecutionId;
  @NotNull String stateExecutionInstanceId;
  @NotNull ExecutionStatus status;

  public static StateStatusUpdateInfo buildFromStateExecutionInstance(
      StateExecutionInstance stateExecutionInstance, boolean isResumed) {
    return builder()
        .workflowExecutionId(stateExecutionInstance.getExecutionUuid())
        .appId(stateExecutionInstance.getAppId())
        .stateExecutionInstanceId(stateExecutionInstance.getUuid())
        .status(isResumed ? ExecutionStatus.RESUMED : stateExecutionInstance.getStatus())
        .build();
  }
}
