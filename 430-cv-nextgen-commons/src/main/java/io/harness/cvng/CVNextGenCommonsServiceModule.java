package io.harness.cvng;

import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.impl.VerificationServiceSecretManagerImpl;

import com.google.inject.AbstractModule;

public class CVNextGenCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(VerificationServiceSecretManager.class).to(VerificationServiceSecretManagerImpl.class);
  }
}
