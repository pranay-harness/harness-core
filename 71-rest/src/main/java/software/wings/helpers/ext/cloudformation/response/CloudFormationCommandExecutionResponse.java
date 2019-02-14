package software.wings.helpers.ext.cloudformation.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudFormationCommandExecutionResponse implements ResponseData {
  private CloudFormationCommandResponse commandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}