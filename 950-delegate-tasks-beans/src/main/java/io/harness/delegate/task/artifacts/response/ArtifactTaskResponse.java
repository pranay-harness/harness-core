package io.harness.delegate.task.artifacts.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.eraro.ErrorCode;
import io.harness.logging.CommandExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ArtifactTaskResponse implements DelegateTaskNotifyResponseData {
  private String errorMessage;
  private ErrorCode errorCode;
  private CommandExecutionStatus commandExecutionStatus;
  private DelegateMetaInfo delegateMetaInfo;
  private ArtifactTaskExecutionResponse artifactTaskExecutionResponse;
}
