package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sClientKeyCert")
public class K8sClientKeyCert implements KubernetesAuth {
  String caCertRef;
  String clientCertRef;
  String clientKeyRef;
  String clientKeyPassphraseRef;
  String clientKeyAlgo;
}
