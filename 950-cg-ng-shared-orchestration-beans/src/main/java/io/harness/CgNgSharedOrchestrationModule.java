package io.harness;

import io.harness.beans.shared.RestraintService;
import io.harness.beans.shared.RestraintServiceImpl;

import com.google.inject.AbstractModule;

public class CgNgSharedOrchestrationModule extends AbstractModule {
  private static CgNgSharedOrchestrationModule instance;

  public static CgNgSharedOrchestrationModule getInstance() {
    if (instance == null) {
      instance = new CgNgSharedOrchestrationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(RestraintService.class).to(RestraintServiceImpl.class);
  }
}
