package software.wings.helpers.ext.pcf.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

@Data
@AllArgsConstructor
public class PcfCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
