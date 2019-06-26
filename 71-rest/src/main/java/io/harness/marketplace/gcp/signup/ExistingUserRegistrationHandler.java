package io.harness.marketplace.gcp.signup;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.MarketPlace;
import software.wings.dl.WingsPersistence;
import software.wings.security.SecretManager;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.security.authentication.SimpleUrlBuilder;
import software.wings.service.intfc.AccountService;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Singleton
public class ExistingUserRegistrationHandler implements GcpMarketplaceSignUpHandler {
  @Inject private AccountService accountService;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;

  @Override
  public URI signUp(MarketPlace marketPlace) {
    String harnessAccountId = marketPlace.getAccountId();
    Account account = accountService.get(harnessAccountId);
    String baseUrl = authenticationUtils.getBaseUrl() + "#/login";

    try {
      String redirectUrl = new SimpleUrlBuilder(baseUrl).addQueryParam("accountName", account.getAccountName()).build();

      logger.info(
          "This customer had subscribed via GCP before. Redirecting to Login page. accountId={}, gcpAccountId={}, accountName={}, redirectUrl={}",
          harnessAccountId, account.getAccountName(), marketPlace.getCustomerIdentificationCode(), redirectUrl);

      return new URI(redirectUrl);
    } catch (URISyntaxException e) {
      logger.error("URISyntaxException when trying to create redirect URL. Base URL: {}", baseUrl, e);
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "Error redirecting to login page. Contact Harness support at support@harness.io ");
    }
  }
}
