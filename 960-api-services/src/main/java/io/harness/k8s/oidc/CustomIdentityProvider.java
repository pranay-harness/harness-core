/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.oidc;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import lombok.Data;

@Data
public class CustomIdentityProvider extends DefaultApi20 {
  private String accessTokenEndpoint;
  private String authorizationBaseUrl;
  private String revokeTokenEndpoint;
  protected CustomIdentityProvider() {}

  public static CustomIdentityProvider instance() {
    return CustomIdentityProvider.InstanceHolder.INSTANCE;
  }

  @Override
  public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
    return OpenIdJsonTokenExtractor.instance();
  }

  private static class InstanceHolder {
    private static final CustomIdentityProvider INSTANCE = new CustomIdentityProvider();

    private InstanceHolder() {}
  }
}
