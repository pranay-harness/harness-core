package io.harness.cdng;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.core.NGCorePersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class CDNGPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NGCorePersistenceConfig.class, TimeoutEnginePersistenceConfig.class,
        OrchestrationPersistenceConfig.class, ConnectorPersistenceConfig.class, CDNGPersistenceConfig.class};
  }
}
