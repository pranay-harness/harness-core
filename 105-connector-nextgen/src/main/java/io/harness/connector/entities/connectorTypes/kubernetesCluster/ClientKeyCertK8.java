package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("clientKeyCertK8")
public class ClientKeyCertK8 extends KubernetesAuth {
  String clientCert;
  String clientKey;
  String clientKeyPassphrase;
  String clientKeyAlgo;
}
