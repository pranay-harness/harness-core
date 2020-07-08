package io.harness.service;

import com.google.inject.AbstractModule;

import io.harness.service.impl.DelegateTaskServiceImpl;
import io.harness.service.intfc.DelegateTaskService;

public class DelegateServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateTaskService.class).to(DelegateTaskServiceImpl.class);
  }
}
