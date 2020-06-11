package io.harness.cvng;

import com.google.inject.AbstractModule;

import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.impl.MetricPackServiceImpl;

public class CVNextGenCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CVConfigService.class).to(CVConfigServiceImpl.class);
    bind(MetricPackService.class).to(MetricPackServiceImpl.class);
    bind(DSConfigService.class).to(DSConfigServiceImpl.class);
  }
}