package software.wings.helpers.ext.k8s.response;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.DelegateMetaInfo;
import io.harness.delegate.task.protocol.DelegateTaskNotifyResponseData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sTaskExecutionResponse implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private K8sTaskResponse k8sTaskResponse;
  private String errorMessage;
  private CommandExecutionStatus commandExecutionStatus;
}
