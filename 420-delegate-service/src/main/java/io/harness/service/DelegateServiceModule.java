package io.harness.service;

import io.harness.service.impl.DelegateCallbackRegistryImpl;
import io.harness.service.impl.DelegateTaskSelectorMapServiceImpl;
import io.harness.service.impl.DelegateTaskServiceImpl;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import io.harness.service.intfc.DelegateTaskService;

import com.google.inject.AbstractModule;

public class DelegateServiceModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateTaskService.class).to(DelegateTaskServiceImpl.class);
    bind(DelegateCallbackRegistry.class).to(DelegateCallbackRegistryImpl.class);
    bind(DelegateTaskSelectorMapService.class).to(DelegateTaskSelectorMapServiceImpl.class);
  }
}
