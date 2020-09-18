package io.harness.cdng;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.core.NGCorePersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class CDNGPersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NGCorePersistenceConfig.class, TimeoutEnginePersistenceConfig.class,
        OrchestrationPersistenceConfig.class, ConnectorPersistenceConfig.class, CDNGPersistenceConfig.class};
  }
}
