/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
