package io.harness.app.impl;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.app.CIManagerPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class CIManagerPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        CIManagerPersistenceConfig.class, CIExecutionPersistenceConfig.class};
  }
}
