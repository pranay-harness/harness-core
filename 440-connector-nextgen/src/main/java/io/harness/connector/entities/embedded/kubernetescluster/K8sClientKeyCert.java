package io.harness.connector.entities.embedded.kubernetescluster;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert")
@OwnedBy(DX)
public class K8sClientKeyCert implements KubernetesAuth {
  String caCertRef;
  String clientCertRef;
  String clientKeyRef;
  String clientKeyPassphraseRef;
  String clientKeyAlgo;
}
