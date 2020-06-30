package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import io.harness.connector.common.kubernetes.KubernetesAuthType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubernetesClusterDetails implements KubernetesCredential {
  String masterUrl;
  KubernetesAuthType authType;
  KubernetesAuth auth;
}
