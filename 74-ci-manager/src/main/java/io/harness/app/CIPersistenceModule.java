package io.harness.app;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;
import software.wings.app.WingsPersistenceConfig;

public class CIPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class,
        CIManagerPersistenceConfig.class, CIExecutionPersistenceConfig.class, CIExecutionPersistenceConfig.class};
  }
}
