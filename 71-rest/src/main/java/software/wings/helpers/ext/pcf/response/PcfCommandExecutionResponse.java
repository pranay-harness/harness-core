package software.wings.helpers.ext.pcf.response;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfCommandExecutionResponse implements ResponseData {
  private PcfCommandResponse pcfCommandResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
