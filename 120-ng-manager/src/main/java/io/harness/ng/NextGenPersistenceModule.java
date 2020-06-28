package io.harness.ng;

import io.harness.connector.ConnectorPersistenceConfig;

public class NextGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {ConnectorPersistenceConfig.class, NextGenPersistenceConfig.class};
  }
}
