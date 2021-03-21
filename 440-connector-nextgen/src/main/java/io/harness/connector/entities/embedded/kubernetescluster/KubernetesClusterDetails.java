package io.harness.connector.entities.embedded.kubernetescluster;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails")
@OwnedBy(DX)
public class KubernetesClusterDetails implements KubernetesCredential {
  String masterUrl;
  KubernetesAuthType authType;
  KubernetesAuth auth;
}
