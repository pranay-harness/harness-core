package io.harness.delegate.task.azure.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSTaskExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private AzureVMSSTaskResponse azureVMSSTaskResponse;
  private String errorMessage;
  private CommandExecutionResult.CommandExecutionStatus commandExecutionStatus;
}
