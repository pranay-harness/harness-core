package io.harness.gitsync;

import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class GitSyncPersistenceTestConfig extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {GitSyncPersistenceConfig.class};
  }
}
