package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import io.harness.connector.common.kubernetes.KubernetesCredentialType;
import io.harness.connector.entities.Connector;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "KubernetesClusterConfigKeys")
public class KubernetesClusterConfig extends Connector {
  KubernetesCredentialType credentialType;
  KubernetesCredential credential;
}