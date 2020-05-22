package io.harness.cvng;

import com.google.inject.AbstractModule;

public class CVNextGenCommonsServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(CVConfigService.class).to(CVConfigServiceImpl.class);
  }
}