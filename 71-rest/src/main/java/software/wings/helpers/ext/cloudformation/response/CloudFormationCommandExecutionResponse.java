package software.wings.helpers.ext.cloudformation.response;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloudFormationCommandExecutionResponse implements DelegateResponseData {
  private CloudFormationCommandResponse commandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
