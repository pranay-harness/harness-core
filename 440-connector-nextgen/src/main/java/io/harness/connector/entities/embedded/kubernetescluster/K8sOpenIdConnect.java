package io.harness.connector.entities.embedded.kubernetescluster;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect")
@OwnedBy(DX)
public class K8sOpenIdConnect implements KubernetesAuth {
  String oidcIssuerUrl;
  String oidcClientIdRef;
  String oidcUsername;
  String oidcUsernameRef;
  String oidcPasswordRef;
  String oidcSecretRef;
  String oidcScopes;
}
