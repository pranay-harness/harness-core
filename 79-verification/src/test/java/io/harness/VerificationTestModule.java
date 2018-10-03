package io.harness;

import com.google.inject.AbstractModule;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientFactory;
import io.harness.security.VerificationTokenGenerator;
import software.wings.utils.WingsIntegrationTestConstants;

public class VerificationTestModule extends AbstractModule {
  @Override
  protected void configure() {
    VerificationTokenGenerator tokenGenerator = new VerificationTokenGenerator();
    bind(VerificationTokenGenerator.class).toInstance(tokenGenerator);
    bind(VerificationManagerClient.class)
        .toProvider(new VerificationManagerClientFactory(WingsIntegrationTestConstants.API_BASE + "/", tokenGenerator));
  }
}
