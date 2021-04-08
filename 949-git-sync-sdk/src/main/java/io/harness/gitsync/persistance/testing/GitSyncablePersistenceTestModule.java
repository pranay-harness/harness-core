package io.harness.gitsync.persistance.testing;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.springdata.SpringPersistenceTestConfig;

// This module is only for test configs
@OwnedBy(HarnessTeam.DX)
public class GitSyncablePersistenceTestModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {GitSyncablePersistenceTestConfig.class, SpringPersistenceTestConfig.class};
  }
}
