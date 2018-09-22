package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

@Data
@Builder
public class CloudFormationCommandExecutionResponse implements NotifyResponseData {
  private CloudFormationCommandResponse commandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}