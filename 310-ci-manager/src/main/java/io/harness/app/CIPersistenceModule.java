package io.harness.app;

import io.harness.pms.sdk.core.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

public class CIPersistenceModule extends SpringPersistenceModule {
  private final boolean withPMS;

  public CIPersistenceModule(boolean withPMS) {
    this.withPMS = withPMS;
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    Class<?>[] resultClasses;
    if (withPMS) {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class, PmsSdkPersistenceConfig.class};
    } else {
      resultClasses = new Class<?>[] {SpringPersistenceConfig.class};
    }
    return resultClasses;
  }
}
