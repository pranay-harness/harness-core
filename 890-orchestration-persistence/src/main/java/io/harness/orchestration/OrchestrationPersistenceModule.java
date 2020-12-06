package io.harness.orchestration;

import com.google.inject.AbstractModule;

public class OrchestrationPersistenceModule extends AbstractModule {
  private static OrchestrationPersistenceModule instance;

  public static synchronized OrchestrationPersistenceModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationPersistenceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
