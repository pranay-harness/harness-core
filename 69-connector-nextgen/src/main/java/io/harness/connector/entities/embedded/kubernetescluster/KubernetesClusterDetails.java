package io.harness.connector.entities.embedded.kubernetescluster;

import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("clusterDetails")
public class KubernetesClusterDetails implements KubernetesCredential {
  String masterUrl;
  KubernetesAuthType authType;
  KubernetesAuth auth;
}
