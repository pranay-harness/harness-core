package io.harness.connector;

import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class ConnectorPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {ConnectorPersistenceConfig.class};
  }
}
