package io.harness.delegate.task.azure;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureTaskExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
  private AzureTaskResponse azureTaskResponse;
}
