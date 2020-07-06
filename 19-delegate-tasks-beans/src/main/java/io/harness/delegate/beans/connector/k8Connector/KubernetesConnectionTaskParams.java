package io.harness.delegate.beans.connector.k8Connector;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesConnectionTaskParams implements TaskParameters {
  KubernetesClusterConfigDTO kubernetesClusterConfig;
}
