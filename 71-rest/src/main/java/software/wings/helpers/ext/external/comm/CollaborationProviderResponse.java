package software.wings.helpers.ext.external.comm;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollaborationProviderResponse implements ResponseData {
  private String output;
  private CommandExecutionStatus status;
  private String errorMessage;
  private String accountId;
}
