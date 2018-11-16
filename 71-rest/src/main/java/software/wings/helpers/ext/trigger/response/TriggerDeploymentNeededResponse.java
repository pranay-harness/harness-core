package software.wings.helpers.ext.trigger.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.ExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerDeploymentNeededResponse extends TriggerResponse {
  private boolean deploymentNeeded;

  @Builder
  public TriggerDeploymentNeededResponse(ExecutionStatus executionStatus, String errorMsg, boolean deploymentNeeded) {
    super(executionStatus, errorMsg);
    this.deploymentNeeded = deploymentNeeded;
  }
}
