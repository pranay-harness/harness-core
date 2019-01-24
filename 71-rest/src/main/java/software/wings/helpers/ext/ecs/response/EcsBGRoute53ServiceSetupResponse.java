package software.wings.helpers.ext.ecs.response;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@EqualsAndHashCode(callSuper = false)
public class EcsBGRoute53ServiceSetupResponse extends EcsCommandResponse {
  @Builder
  public EcsBGRoute53ServiceSetupResponse(CommandExecutionStatus commandExecutionStatus, String output) {
    super(commandExecutionStatus, output);
  }
}