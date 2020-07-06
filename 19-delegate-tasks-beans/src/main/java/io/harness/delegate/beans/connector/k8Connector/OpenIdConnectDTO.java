package io.harness.delegate.beans.connector.k8Connector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OpenIdConnectDTO extends KubernetesAuthCredentialDTO {
  @JsonProperty("oidc-issuer-url") String oidcIssuerUrl;
  @JsonProperty("oidc-client-id") String oidcClientId;
  @JsonProperty("oidc-username") String oidcUsername;
  @JsonProperty("oidc-password") String oidcPassword;
  @JsonProperty("oidc-secret") String oidcSecret;
  @JsonProperty("oidc-scopes") String oidcScopes;
}
