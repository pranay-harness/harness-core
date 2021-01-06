package io.harness.delegate.command;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.shell.CommandExecutionData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommandExecutionResult implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus status;
  private CommandExecutionData commandExecutionData;
  private String errorMessage;
}
