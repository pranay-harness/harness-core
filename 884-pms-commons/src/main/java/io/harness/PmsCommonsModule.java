package io.harness;

import io.harness.orchestration.OrchestrationPersistenceModule;

import com.google.inject.AbstractModule;

public class PmsCommonsModule extends AbstractModule {
  private static PmsCommonsModule instance;

  public static PmsCommonsModule getInstance() {
    if (instance == null) {
      instance = new PmsCommonsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(OrchestrationPersistenceModule.getInstance());
  }
}
