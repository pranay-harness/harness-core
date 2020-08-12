package io.harness.delegate.task.k8s;

import io.harness.delegate.beans.ResponseData;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sDeployResponse implements ResponseData {
  CommandExecutionStatus commandExecutionStatus;
  String errorMessage;
  K8sNGTaskResponse k8sNGTaskResponse;
}
