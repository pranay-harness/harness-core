package io.harness.delegate.k8s;

import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.k8s.model.K8sDelegateTaskParams;

public class K8sRollingRequestHandler extends K8sRequestHandler {
  @Override
  protected K8sDeployResponse executeTaskInternal(
      K8sDeployRequest k8sDeployRequest, K8sDelegateTaskParams k8SDelegateTaskParams) {
    return null;
  }
}
