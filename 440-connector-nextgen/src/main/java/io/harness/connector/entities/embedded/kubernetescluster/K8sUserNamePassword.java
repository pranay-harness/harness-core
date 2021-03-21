package io.harness.connector.entities.embedded.kubernetescluster;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword")
@OwnedBy(DX)
public class K8sUserNamePassword implements KubernetesAuth {
  String userName;
  String userNameRef;
  String passwordRef;
}
