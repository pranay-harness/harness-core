package io.harness.service;

import io.harness.govern.DependencyModule;
import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateSyncService;

import java.util.Set;

public class DelegateServiceDriverModule extends DependencyModule {
  private static volatile DelegateServiceDriverModule instance;

  public static DelegateServiceDriverModule getInstance() {
    if (instance == null) {
      instance = new DelegateServiceDriverModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(DelegateSyncService.class).to(DelegateSyncServiceImpl.class);
    bind(DelegateAsyncService.class).to(DelegateAsyncServiceImpl.class);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
