package software.wings.verification;

import com.google.inject.AbstractModule;

import io.harness.security.VerificationTokenGenerator;

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
    VerificationTokenGenerator tokenGenerator = new VerificationTokenGenerator();
    bind(VerificationTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationServiceClient.class).toProvider(new VerificationServiceClientFactory(baseUrl, tokenGenerator));
  }
}
