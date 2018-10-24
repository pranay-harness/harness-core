package software.wings.helpers.ext.k8s.response;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@Builder
public class K8sCommandExecutionResponse implements ResponseData {
  private K8sCommandResponse k8sCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
