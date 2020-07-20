package io.harness.cdng;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class CDNGPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {
        ConnectorPersistenceConfig.class, CDNGPersistenceConfig.class, OrchestrationPersistenceConfig.class};
  }
}
