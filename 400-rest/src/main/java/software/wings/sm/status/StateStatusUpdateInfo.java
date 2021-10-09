package software.wings.sm.status;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.StateExecutionInstance;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
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
