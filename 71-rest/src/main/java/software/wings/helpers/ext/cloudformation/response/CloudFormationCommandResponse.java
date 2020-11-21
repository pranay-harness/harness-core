package software.wings.helpers.ext.cloudformation.response;

import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CloudFormationCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
