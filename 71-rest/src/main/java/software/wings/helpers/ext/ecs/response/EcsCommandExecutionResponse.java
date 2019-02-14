package software.wings.helpers.ext.ecs.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsCommandExecutionResponse implements ResponseData {
  private EcsCommandResponse ecsCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}