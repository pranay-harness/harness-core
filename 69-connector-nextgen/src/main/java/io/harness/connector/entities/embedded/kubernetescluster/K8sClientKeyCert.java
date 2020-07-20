package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("clientKeyCertK8")
public class K8sClientKeyCert implements KubernetesAuth {
  String clientCert;
  String clientKey;
  String clientKeyPassphrase;
  String clientKeyAlgo;
}
