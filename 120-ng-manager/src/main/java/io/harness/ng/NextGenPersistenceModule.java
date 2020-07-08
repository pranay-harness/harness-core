package io.harness.ng;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;

public class NextGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {
        OrchestrationPersistenceConfig.class, ConnectorPersistenceConfig.class, NextGenPersistenceConfig.class};
  }
}
