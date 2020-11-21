package io.harness.managerclient;

import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;

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
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(managerBaseUrl, tokenGenerator));
  }
}
