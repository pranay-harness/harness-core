package software.wings.helpers.ext.external.comm;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

@Data
@Builder
public class CollaborationProviderResponse implements NotifyResponseData {
  private String output;
  private CommandExecutionStatus status;
  private String errorMessage;
  private String accountId;
}
