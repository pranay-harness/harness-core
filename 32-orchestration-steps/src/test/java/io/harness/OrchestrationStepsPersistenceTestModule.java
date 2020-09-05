package io.harness;

import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class OrchestrationStepsPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class};
  }
}
