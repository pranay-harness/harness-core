package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDC)
public class WebHookTriggerResponseData implements DelegateTaskNotifyResponseData {
  private boolean isWebhookAuthenticated;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private DelegateMetaInfo delegateMetaInfo;
}
