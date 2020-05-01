package software.wings.delegatetasks.citasks;

import software.wings.beans.ci.ExecuteCommandTaskParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

public interface ExecuteCommandTaskHandler {
  enum Type { K8 }
  ExecuteCommandTaskHandler.Type getType();
  K8sTaskExecutionResponse executeTaskInternal(ExecuteCommandTaskParams executeCommandTaskParams);
}