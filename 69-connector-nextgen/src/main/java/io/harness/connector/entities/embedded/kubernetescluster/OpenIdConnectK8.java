package io.harness.connector.entities.embedded.kubernetescluster;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("openIdConnectK8")
public class OpenIdConnectK8 implements KubernetesAuth {
  String oidcIssuerUrl;
  String oidcClientId;
  String oidcUsername;
  String oidcPassword;
  String oidcSecret;
  String oidcScopes;
}
