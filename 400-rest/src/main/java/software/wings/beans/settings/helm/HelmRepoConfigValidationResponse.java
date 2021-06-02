package software.wings.beans.settings.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class HelmRepoConfigValidationResponse implements DelegateTaskNotifyResponseData {
  private String errorMessage;
  private ErrorCode errorCode;
  private CommandExecutionStatus commandExecutionStatus;
  private DelegateMetaInfo delegateMetaInfo;
}
