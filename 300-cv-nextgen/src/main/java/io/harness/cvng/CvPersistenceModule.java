package io.harness.cvng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelPersistenceConfig;
import io.harness.pms.sdk.PmsSdkPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.springdata.SpringPersistenceModule;
@OwnedBy(HarnessTeam.CV)
public class CvPersistenceModule extends SpringPersistenceModule {
  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class<?>[] {
        SpringPersistenceConfig.class, PmsSdkPersistenceConfig.class, NotificationChannelPersistenceConfig.class};
  }
}
