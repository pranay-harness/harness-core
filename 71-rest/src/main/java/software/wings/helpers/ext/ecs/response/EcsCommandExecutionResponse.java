package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@Builder
public class EcsCommandExecutionResponse implements ResponseData {
  private EcsCommandResponse ecsCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}