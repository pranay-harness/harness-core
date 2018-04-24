package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

@Data
@Builder
public class PcfCommandExecutionResponse implements NotifyResponseData {
  private PcfCommandResponse pcfCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
