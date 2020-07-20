package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("userNamePasswordK8")
public class K8sUserNamePassword implements KubernetesAuth {
  String userName;
  String password;
  String cacert;
}
