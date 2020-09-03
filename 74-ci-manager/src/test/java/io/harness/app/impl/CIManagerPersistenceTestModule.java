package io.harness.app.impl;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.app.CIManagerPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class CIManagerPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {
        OrchestrationPersistenceConfig.class, CIManagerPersistenceConfig.class, CIExecutionPersistenceConfig.class};
  }
}
