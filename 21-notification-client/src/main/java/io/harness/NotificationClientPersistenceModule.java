package io.harness;

import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class NotificationClientPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NotificationChannelPersistenceConfig.class};
  }
}