package software.wings.helpers.ext.external.comm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollaborationProviderResponse implements DelegateTaskNotifyResponseData {
  private String output;
  private CommandExecutionStatus status;
  private String errorMessage;
  private String accountId;
  private DelegateMetaInfo delegateMetaInfo;
}
