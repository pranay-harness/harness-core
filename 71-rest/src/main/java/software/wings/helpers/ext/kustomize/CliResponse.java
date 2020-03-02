package software.wings.helpers.ext.kustomize;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
class CliResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
