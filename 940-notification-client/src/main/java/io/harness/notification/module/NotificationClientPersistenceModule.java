package io.harness.notification.module;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.PersistenceModule;

public class NotificationClientPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {NotificationChannelPersistenceConfig.class};
  }
}