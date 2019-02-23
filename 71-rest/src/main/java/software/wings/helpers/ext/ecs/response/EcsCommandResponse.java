package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EcsCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
