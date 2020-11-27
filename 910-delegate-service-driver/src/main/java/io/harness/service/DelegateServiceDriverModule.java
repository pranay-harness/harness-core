package io.harness.service;

import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateProgressServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateProgressService;
import io.harness.service.intfc.DelegateSyncService;

import com.google.inject.AbstractModule;

public class DelegateServiceDriverModule extends AbstractModule {
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
    bind(DelegateProgressService.class).to(DelegateProgressServiceImpl.class);
  }
}
