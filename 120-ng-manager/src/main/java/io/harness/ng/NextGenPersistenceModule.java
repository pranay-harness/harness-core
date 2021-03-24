package io.harness.ng;

import io.harness.ng.accesscontrol.migrations.MigrationPersistenceConfig;
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
          SpringPersistenceConfig.class, MigrationPersistenceConfig.class, PmsSdkPersistenceConfig.class};
    } else {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class, MigrationPersistenceConfig.class};
    }
    return resultClasses;
  }
}
