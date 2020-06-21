package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KubernetesDelegateDetails implements KubernetesCredential {
  String delegateName;
}
