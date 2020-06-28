package io.harness.app.impl;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import software.wings.app.WingsPersistenceConfig;

public class CIManagerPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class};
  }
}
