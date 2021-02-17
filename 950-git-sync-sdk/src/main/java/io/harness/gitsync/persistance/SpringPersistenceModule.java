package io.harness.gitsync.persistance;

import io.harness.springdata.PersistenceModule;

public class SpringPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistanceConfig.class};
  }
}