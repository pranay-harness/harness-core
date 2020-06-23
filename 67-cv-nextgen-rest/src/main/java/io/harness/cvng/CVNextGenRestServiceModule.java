package io.harness.cvng;

import com.google.inject.AbstractModule;

import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.impl.DSConfigServiceImpl;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;
import io.harness.cvng.core.services.impl.VerificationServiceSecretManagerImpl;

public class CVNextGenRestServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new CVNextGenCommonsServiceModule());
    bind(MetricPackService.class).to(MetricPackServiceImpl.class);
    bind(DSConfigService.class).to(DSConfigServiceImpl.class);
    bind(VerificationServiceSecretManager.class).to(VerificationServiceSecretManagerImpl.class);
  }
}