package software.wings.helpers.ext.trigger.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerDeploymentNeededResponse extends TriggerResponse {
  private boolean deploymentNeeded;

  @Builder
  public TriggerDeploymentNeededResponse(
      CommandExecutionStatus commandExecutionStatus, String output, boolean deploymentNeeded) {
    super(commandExecutionStatus, output);
    this.deploymentNeeded = deploymentNeeded;
  }
}
