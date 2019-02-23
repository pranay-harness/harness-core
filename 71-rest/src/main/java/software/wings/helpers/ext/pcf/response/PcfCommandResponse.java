package software.wings.helpers.ext.pcf.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PcfCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
