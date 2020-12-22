package io.harness.service;

import io.harness.service.impl.PmsDelegateAsyncServiceImpl;
import io.harness.service.impl.PmsDelegateProgressServiceImpl;
import io.harness.service.impl.PmsDelegateSyncServiceImpl;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateProgressService;
import io.harness.service.intfc.DelegateSyncService;

import com.google.inject.AbstractModule;

public class PmsDelegateServiceDriverModule extends AbstractModule {
  private static volatile PmsDelegateServiceDriverModule instance;

  public static PmsDelegateServiceDriverModule getInstance() {
    if (instance == null) {
      instance = new PmsDelegateServiceDriverModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(DelegateSyncService.class).to(PmsDelegateSyncServiceImpl.class);
    bind(DelegateAsyncService.class).to(PmsDelegateAsyncServiceImpl.class);
    bind(DelegateProgressService.class).to(PmsDelegateProgressServiceImpl.class);
  }
}
