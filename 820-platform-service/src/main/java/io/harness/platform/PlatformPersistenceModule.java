package io.harness.platform;

import io.harness.springdata.PersistenceModule;

public class PlatformPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {PlatformPersistenceConfig.class};
  }
}
