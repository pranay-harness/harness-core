package io.harness.cdng.connectornextgen.service;

import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

public interface KubernetesConnectorService {
  @DelegateTaskType(TaskType.VALIDATE_KUBERNETES_CONFIG)
  boolean validate(KubernetesClusterConfigDTO kubernetesClusterConfigDTO);
}
