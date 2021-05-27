package io.harness.pms.sample.cd;

import io.harness.pms.sdk.core.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

public class CdPersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {SpringPersistenceConfig.class, PmsSdkPersistenceConfig.class};
  }
}
