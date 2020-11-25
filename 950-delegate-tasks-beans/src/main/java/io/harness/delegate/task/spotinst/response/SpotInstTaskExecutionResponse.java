package io.harness.delegate.task.spotinst.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpotInstTaskExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private SpotInstTaskResponse spotInstTaskResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
