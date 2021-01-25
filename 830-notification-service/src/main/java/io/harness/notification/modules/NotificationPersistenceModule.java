package io.harness.notification.modules;

import io.harness.notification.NotificationChannelServicePersistenceConfig;
import io.harness.notification.NotificationPersistenceConfig;
import io.harness.springdata.PersistenceModule;

public class NotificationPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {NotificationChannelServicePersistenceConfig.class, NotificationPersistenceConfig.class};
  }
}
