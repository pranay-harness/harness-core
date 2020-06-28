package io.harness.rule;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class OrchestrationPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class};
  }
}
