package io.harness.managerclient;

import com.google.inject.AbstractModule;

import io.harness.security.TokenGenerator;
import io.harness.verification.VerificationServiceClient;
import io.harness.verification.VerificationServiceClientFactory;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class ManagerClientModule extends AbstractModule {
  private final String managerBaseUrl;
  private final String accountId;
  private final String accountSecret;
  private final String verificationServiceBaseUrl;

  public ManagerClientModule(
      String managerBaseUrl, String verificationServiceBaseUrl, String accountId, String accountSecret) {
    this.managerBaseUrl = managerBaseUrl;
    this.verificationServiceBaseUrl = verificationServiceBaseUrl;
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  @Override
  protected void configure() {
    TokenGenerator tokenGenerator = new TokenGenerator(accountId, accountSecret);
    bind(TokenGenerator.class).toInstance(tokenGenerator);
    bind(ManagerClient.class).toProvider(new ManagerClientFactory(managerBaseUrl, tokenGenerator));
    bind(VerificationServiceClient.class)
        .toProvider(new VerificationServiceClientFactory(verificationServiceBaseUrl, tokenGenerator));
  }
}
