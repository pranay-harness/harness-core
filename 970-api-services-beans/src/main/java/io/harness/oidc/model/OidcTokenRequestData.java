package io.harness.oidc.model;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString(exclude = {"password", "clientId", "clientSecret"})
public class OidcTokenRequestData {
  private String providerUrl;
  private String username;
  private String password;
  private String clientId;
  private String clientSecret;
  private String grantType;
  private String scope;
}
