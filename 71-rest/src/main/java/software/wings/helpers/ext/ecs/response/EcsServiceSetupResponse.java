package software.wings.helpers.ext.ecs.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsServiceSetupResponse extends EcsCommandResponse {
  private boolean isBlueGreen;

  @Builder
  public EcsServiceSetupResponse(CommandExecutionStatus commandExecutionStatus, String output, boolean isBlueGreen) {
    super(commandExecutionStatus, output);
    this.isBlueGreen = isBlueGreen;
  }
}
