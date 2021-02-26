package io.harness;

import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;

public class PipelinePersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {SpringPersistenceConfig.class, NotificationChannelPersistenceConfig.class};
  }
}
