/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
