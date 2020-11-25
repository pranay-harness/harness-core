package io.harness.notification.modules;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.PersistenceModule;

public class NotificationPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {NotificationChannelPersistenceConfig.class};
  }
}
