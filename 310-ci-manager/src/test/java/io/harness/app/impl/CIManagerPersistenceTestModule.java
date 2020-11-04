package io.harness.app.impl;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.app.CIManagerPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class CIManagerPersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        CIManagerPersistenceConfig.class, CIExecutionPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class};
  }
}
