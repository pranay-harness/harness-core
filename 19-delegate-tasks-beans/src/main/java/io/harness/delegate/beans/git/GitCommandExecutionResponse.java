package io.harness.delegate.beans.git;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitCommandExecutionResponse implements DelegateTaskNotifyResponseData {
  private GitCommandResult gitCommandResult;
  private GitCommandRequest gitCommandRequest;
  private GitCommandStatus gitCommandStatus;
  private String errorMessage;
  private ErrorCode errorCode;
  private DelegateMetaInfo delegateMetaInfo;

  public enum GitCommandStatus { SUCCESS, FAILURE }
}
