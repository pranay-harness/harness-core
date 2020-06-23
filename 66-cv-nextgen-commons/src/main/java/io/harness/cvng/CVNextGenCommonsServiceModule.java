package io.harness.cvng;

import com.google.inject.AbstractModule;

import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.impl.CVConfigServiceImpl;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;
import io.harness.cvng.core.services.impl.VerificationServiceSecretManagerImpl;

public class CVNextGenCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CVConfigService.class).to(CVConfigServiceImpl.class);
    bind(MetricPackService.class).to(MetricPackServiceImpl.class);
    bind(VerificationServiceSecretManager.class).to(VerificationServiceSecretManagerImpl.class);
  }
}