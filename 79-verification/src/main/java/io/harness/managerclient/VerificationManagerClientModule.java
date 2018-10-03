package io.harness.managerclient;

import com.google.inject.AbstractModule;

import io.harness.security.VerificationTokenGenerator;

/**
 * Guice Module for initializing Verification Manager client.
 * Created by raghu on 09/17/18.
 */
public class VerificationManagerClientModule extends AbstractModule {
  private final String managerBaseUrl;

  public VerificationManagerClientModule(String managerBaseUrl) {
    this.managerBaseUrl = managerBaseUrl;
  }

  @Override
  protected void configure() {
    VerificationTokenGenerator tokenGenerator = new VerificationTokenGenerator();
    bind(VerificationTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(managerBaseUrl, tokenGenerator));
  }
}
