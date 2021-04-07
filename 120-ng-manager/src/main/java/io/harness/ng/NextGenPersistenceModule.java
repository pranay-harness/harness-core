package io.harness.ng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.persistance.GitSyncablePersistenceConfig;
import io.harness.ng.accesscontrol.migrations.AccessControlMigrationPersistenceConfig;
import io.harness.pms.sdk.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

@OwnedBy(HarnessTeam.PL)
public class NextGenPersistenceModule extends SpringPersistenceModule {
  private final boolean withPMS;

  public NextGenPersistenceModule(boolean withPMS) {
    this.withPMS = withPMS;
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    Class<?>[] resultClasses;
    if (withPMS) {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class, PmsSdkPersistenceConfig.class,
          AccessControlMigrationPersistenceConfig.class, GitSyncablePersistenceConfig.class};
    } else {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class, AccessControlMigrationPersistenceConfig.class,
          GitSyncablePersistenceConfig.class};
    }
    return resultClasses;
  }
}
