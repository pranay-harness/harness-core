package io.harness.notification.modules;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.notification.NotificationPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class NotificationPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {NotificationPersistenceConfig.class, NotificationChannelPersistenceConfig.class};
  }
}
