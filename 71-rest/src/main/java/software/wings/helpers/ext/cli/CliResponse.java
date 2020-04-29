package software.wings.helpers.ext.cli;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CliResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
