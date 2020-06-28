package io.harness.app;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.ng.PersistenceModule;
import io.harness.ng.SpringPersistenceConfig;
import software.wings.app.WingsPersistenceConfig;

public class CIPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class};
  }
}
