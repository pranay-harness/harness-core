package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
public class K8sDeployResponse implements DelegateTaskNotifyResponseData {
  CommandExecutionStatus commandExecutionStatus;
  String errorMessage;
  K8sNGTaskResponse k8sNGTaskResponse;
  @NonFinal CommandUnitsProgress commandUnitsProgress;
  @NonFinal DelegateMetaInfo delegateMetaInfo;

  @Override
  public void setDelegateMetaInfo(DelegateMetaInfo metaInfo) {
    this.delegateMetaInfo = metaInfo;
  }

  public void setCommandUnitsProgress(CommandUnitsProgress commandUnitsProgress) {
    this.commandUnitsProgress = commandUnitsProgress;
  }
}
