package software.wings.delegatetasks.citasks;

import software.wings.beans.ci.CICleanupTaskParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

public interface CICleanupTaskHandler {
  enum Type { GCP_K8 }

  CICleanupTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(CICleanupTaskParams ciCleanupTaskParams);
}