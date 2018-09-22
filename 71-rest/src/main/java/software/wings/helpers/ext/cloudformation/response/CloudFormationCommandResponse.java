package software.wings.helpers.ext.cloudformation.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@AllArgsConstructor
public class CloudFormationCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}