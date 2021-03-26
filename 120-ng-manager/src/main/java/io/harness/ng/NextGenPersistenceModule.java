package io.harness.ng;

import io.harness.ng.accesscontrol.migrations.AccessControlMigrationPersistenceConfig;
import io.harness.pms.sdk.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

public class NextGenPersistenceModule extends SpringPersistenceModule {
  private final boolean withPMS;

  public NextGenPersistenceModule(boolean withPMS) {
    this.withPMS = withPMS;
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    Class<?>[] resultClasses;
    if (withPMS) {
      resultClasses = new Class<?>[] {
          SpringPersistenceConfig.class, PmsSdkPersistenceConfig.class, AccessControlMigrationPersistenceConfig.class};
    } else {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class, AccessControlMigrationPersistenceConfig.class};
    }
    return resultClasses;
  }
}
