package io.harness.platform;

import io.harness.platform.notification.NotificationPersistenceConfig;
import io.harness.springdata.PersistenceModule;

public class PlatformPersistenceModule extends PersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {NotificationPersistenceConfig.class};
  }
}
