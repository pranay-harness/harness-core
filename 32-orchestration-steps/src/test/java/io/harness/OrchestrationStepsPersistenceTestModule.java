package io.harness;

import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class OrchestrationStepsPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class};
  }
}
