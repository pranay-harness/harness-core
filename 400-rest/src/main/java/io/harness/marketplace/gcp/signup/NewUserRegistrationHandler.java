/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp.signup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;

import software.wings.beans.UserInvite;
import software.wings.beans.marketplace.MarketPlaceConstants;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.security.JWT_CATEGORY;
import software.wings.security.SecretManager;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.SimpleUrlBuilder;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class NewUserRegistrationHandler implements GcpMarketplaceSignUpHandler {
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private UserService userService;
  @Inject private SecretManager secretManager;

  @Override
  public URI signUp(String gcpMarketplaceToken) {
    String baseUrl = authenticationUtils.getBaseUrl() + "#/invite";
    UserInvite userInvite = userService.createUserInviteForMarketPlace();

    String harnessToken = getMarketPlaceToken(userInvite.getUuid(), gcpMarketplaceToken);
    try {
      String redirectUrl = new SimpleUrlBuilder(baseUrl)
                               .addQueryParam("inviteId", userInvite.getUuid())
                               .addQueryParam("marketPlaceToken", harnessToken)
                               .addQueryParam("marketPlaceType", MarketPlaceType.GCP.name())
                               .build();

      return new URI(redirectUrl);
    } catch (URISyntaxException e) {
      throw new UnexpectedException(
          "Error redirecting to sign-up page. Contact Harness support at support@harness.io)", e);
    }
  }

  private String getMarketPlaceToken(String userInviteId, String gcpMarketplaceToken) {
    Map<String, String> claims = new HashMap<>();
    claims.put(MarketPlaceConstants.USERINVITE_ID_CLAIM_KEY, userInviteId);
    claims.put(MarketPlaceConstants.GCP_MARKETPLACE_TOKEN, gcpMarketplaceToken);
    return secretManager.generateJWTToken(claims, JWT_CATEGORY.MARKETPLACE_SIGNUP);
  }
}
