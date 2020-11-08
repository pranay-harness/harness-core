package io.harness.connector;

import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class ConnectorPersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {ConnectorPersistenceConfig.class};
  }
}
