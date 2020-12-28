package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.K8sOpenIdConnect")
public class K8sOpenIdConnect implements KubernetesAuth {
  String oidcIssuerUrl;
  String oidcClientIdRef;
  String oidcUsername;
  String oidcUsernameRef;
  String oidcPasswordRef;
  String oidcSecretRef;
  String oidcScopes;
}
