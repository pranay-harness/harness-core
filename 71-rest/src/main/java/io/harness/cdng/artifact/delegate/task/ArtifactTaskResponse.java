package io.harness.cdng.artifact.delegate.task;

import io.harness.cdng.artifact.bean.ArtifactAttributes;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
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
  private CommandExecutionStatus commandExecutionStatus;
  private DelegateMetaInfo delegateMetaInfo;
  private ArtifactAttributes artifactAttributes;
}
