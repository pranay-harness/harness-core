package software.wings.helpers.ext.external.comm;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
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
