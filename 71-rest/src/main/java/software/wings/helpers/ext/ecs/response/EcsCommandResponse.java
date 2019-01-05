package software.wings.helpers.ext.ecs.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EcsCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
