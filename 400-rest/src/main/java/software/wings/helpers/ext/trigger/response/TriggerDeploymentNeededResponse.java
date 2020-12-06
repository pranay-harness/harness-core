package software.wings.helpers.ext.trigger.response;

import io.harness.beans.ExecutionStatus;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerDeploymentNeededResponse extends TriggerResponse {
  private boolean deploymentNeeded;

  @Builder
  public TriggerDeploymentNeededResponse(ExecutionStatus executionStatus, String errorMsg, boolean deploymentNeeded) {
    super(null, executionStatus, errorMsg);
    this.deploymentNeeded = deploymentNeeded;
  }
}
