package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
