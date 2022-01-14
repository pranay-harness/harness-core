package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 10/27/17.
 */
@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class GitCommandExecutionResponse implements DelegateTaskNotifyResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
