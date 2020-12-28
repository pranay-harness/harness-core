package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword")
public class K8sUserNamePassword implements KubernetesAuth {
  String userName;
  String userNameRef;
  String passwordRef;
}
