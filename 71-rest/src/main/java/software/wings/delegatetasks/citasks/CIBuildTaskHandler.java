package software.wings.delegatetasks.citasks;

import software.wings.beans.ci.CIBuildSetupTaskParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

public interface CIBuildTaskHandler {
  enum Type { GCP_K8 }

  CIBuildTaskHandler.Type getType();

  K8sTaskExecutionResponse executeTaskInternal(CIBuildSetupTaskParams ciBuildSetupTaskParams);
}