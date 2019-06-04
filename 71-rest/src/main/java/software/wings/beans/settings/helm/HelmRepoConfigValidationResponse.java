package software.wings.beans.settings.helm;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmRepoConfigValidationResponse implements DelegateTaskNotifyResponseData {
  private String errorMessage;
  private ErrorCode errorCode;
  private CommandExecutionStatus commandExecutionStatus;
  private DelegateMetaInfo delegateMetaInfo;
}
