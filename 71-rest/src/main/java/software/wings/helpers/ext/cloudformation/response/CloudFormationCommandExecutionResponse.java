package software.wings.helpers.ext.cloudformation.response;

import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@Builder
public class CloudFormationCommandExecutionResponse implements ResponseData {
  private CloudFormationCommandResponse commandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}