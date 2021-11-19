package io.harness.cvng.client;

import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Guice Module for initializing Verification Manager client.
 * Created by raghu on 09/17/18.
 */
public class VerificationManagerClientModule extends AbstractModule {
  private final String managerBaseUrl;

  public VerificationManagerClientModule(String managerBaseUrl) {
    this.managerBaseUrl = managerBaseUrl + (managerBaseUrl.endsWith("/") ? "api/" : "/api/");
  }

  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(managerBaseUrl, tokenGenerator))
        .in(Singleton.class);
  }
}
