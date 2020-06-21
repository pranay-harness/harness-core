package io.harness.connector.apis.dtos.K8Connector;

import lombok.Builder;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonProperty;

@Value
@Builder
public class OpenIdConnectDTO extends KubernetesAuthDTO {
  @JsonProperty("oidc-issuer-url") String oidcIssuerUrl;
  @JsonProperty("oidc-client-id") String oidcClientId;
  @JsonProperty("oidc-username") String oidcUsername;
  @JsonProperty("oidc-password") String oidcPassword;
  @JsonProperty("oidc-secret") String oidcSecret;
  @JsonProperty("oidc-scopes") String oidcScopes;
}
