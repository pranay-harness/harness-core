package io.harness.ng.core;

import io.harness.cdng.CDNGPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.NextGenPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class CorePersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {ConnectorPersistenceConfig.class, NextGenPersistenceConfig.class, CDNGPersistenceConfig.class};
  }
}
