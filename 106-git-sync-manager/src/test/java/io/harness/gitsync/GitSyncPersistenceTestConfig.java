package io.harness.gitsync;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.core.NGCorePersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class GitSyncPersistenceTestConfig extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NGCorePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        GitSyncPersistenceConfig.class, ConnectorPersistenceConfig.class};
  }
}
