package io.harness;

import io.harness.ng.PersistenceModule;
import io.harness.ng.SpringPersistenceConfig;

public class DataGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class};
  }
}
