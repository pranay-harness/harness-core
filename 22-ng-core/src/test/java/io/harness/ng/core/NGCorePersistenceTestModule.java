package io.harness.ng.core;

import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class NGCorePersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NGCorePersistenceConfig.class};
  }
}
