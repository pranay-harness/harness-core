package io.harness.managerclient;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.TokenGenerator;
import io.harness.verificationclient.CVNextGenServiceClient;
import io.harness.verificationclient.CVNextGenServiceClientFactory;

import com.google.inject.AbstractModule;

@TargetModule(Module._420_DELEGATE_AGENT)
public class DelegateManagerClientModule extends AbstractModule {
  private final String managerBaseUrl;
  private final String accountId;
  private final String accountSecret;
  private final String verificationServiceBaseUrl;
  private final String cvNextGenUrl;

  public DelegateManagerClientModule(String managerBaseUrl, String verificationServiceBaseUrl, String cvNextGenUrl,
      String accountId, String accountSecret) {
    this.managerBaseUrl = managerBaseUrl;
    this.verificationServiceBaseUrl = verificationServiceBaseUrl;
    this.cvNextGenUrl = cvNextGenUrl;
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  @Override
  protected void configure() {
    TokenGenerator tokenGenerator = new TokenGenerator(accountId, accountSecret);
    bind(TokenGenerator.class).toInstance(tokenGenerator);
    bind(DelegateAgentManagerClient.class)
        .toProvider(new DelegateAgentManagerClientFactory(managerBaseUrl, tokenGenerator));
    bind(VerificationServiceClient.class)
        .toProvider(new VerificationServiceClientFactory(verificationServiceBaseUrl, tokenGenerator));
    bind(CVNextGenServiceClient.class).toProvider(new CVNextGenServiceClientFactory(cvNextGenUrl, tokenGenerator));
  }
}
