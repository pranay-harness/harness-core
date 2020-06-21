package io.harness.connector.entities.connectorTypes.kubernetesCluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("userNamePasswordK8")
public class UserNamePasswordK8 extends KubernetesAuth {
  String userName;
  String password;
  String cacert;
}
