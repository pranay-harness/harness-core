package io.harness.verification;

import com.google.inject.AbstractModule;

import io.harness.security.ServiceTokenGenerator;

/**
 * Created by raghu on 09/17/18.
 */
public class VerificationServiceClientModule extends AbstractModule {
  private final String baseUrl;

  public VerificationServiceClientModule(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationServiceClient.class).toProvider(new VerificationServiceClientFactory(baseUrl, tokenGenerator));
  }
}
