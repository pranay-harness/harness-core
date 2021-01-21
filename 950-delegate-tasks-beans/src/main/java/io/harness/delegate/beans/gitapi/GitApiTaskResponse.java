package io.harness.delegate.beans.gitapi;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitApiTaskResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private GitApiResult gitApiResult;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
