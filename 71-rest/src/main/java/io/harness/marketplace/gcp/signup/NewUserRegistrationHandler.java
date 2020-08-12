package io.harness.marketplace.gcp.signup;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.MarketPlace;
import software.wings.beans.UserInvite;
import software.wings.beans.marketplace.MarketPlaceConstants;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.security.JWT_CATEGORY;
import software.wings.security.SecretManager;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.SimpleUrlBuilder;
import software.wings.service.intfc.UserService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class NewUserRegistrationHandler implements GcpMarketplaceSignUpHandler {
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private UserService userService;
  @Inject private SecretManager secretManager;

  @Override
  public URI signUp(MarketPlace marketPlace) {
    String baseUrl = authenticationUtils.getBaseUrl() + "#/invite";
    UserInvite userInvite = userService.createUserInviteForMarketPlace();

    String harnessToken = getMarketPlaceToken(userInvite.getUuid(), marketPlace.getUuid(), secretManager);

    try {
      String redirectUrl = new SimpleUrlBuilder(baseUrl)
                               .addQueryParam("inviteId", userInvite.getUuid())
                               .addQueryParam("marketPlaceToken", harnessToken)
                               .addQueryParam("marketPlaceType", MarketPlaceType.GCP.toString())
                               .build();

      return new URI(redirectUrl);
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException when trying to create redirect URL. Base URL: {}", baseUrl, e);
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "Error redirecting to signup page. Contact Harness support at support@harness.io ");
    }
  }

  static String getMarketPlaceToken(String userInviteId, String marketPlaceUuid, SecretManager secretManager) {
    Map<String, String> claims = new HashMap<>();
    claims.put(MarketPlaceConstants.USERINVITE_ID_CLAIM_KEY, userInviteId);
    claims.put(MarketPlaceConstants.MARKETPLACE_ID_CLAIM_KEY, marketPlaceUuid);
    return secretManager.generateJWTToken(claims, JWT_CATEGORY.MARKETPLACE_SIGNUP);
  }
}
