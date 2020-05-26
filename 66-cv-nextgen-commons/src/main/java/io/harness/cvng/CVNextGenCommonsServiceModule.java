package io.harness.cvng;

import com.google.inject.AbstractModule;

import io.harness.cvng.core.services.api.DataSourceService;
import io.harness.cvng.core.services.impl.DataSourceServiceImpl;

public class CVNextGenCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CVConfigService.class).to(CVConfigServiceImpl.class);
    bind(DataSourceService.class).to(DataSourceServiceImpl.class);
  }
}