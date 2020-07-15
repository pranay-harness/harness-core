package io.harness.cdng.connector.service;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

public interface KubernetesConnectorDelegateService {
  @DelegateTaskType(TaskType.VALIDATE_KUBERNETES_CONFIG)
  boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfigDTO);
}
