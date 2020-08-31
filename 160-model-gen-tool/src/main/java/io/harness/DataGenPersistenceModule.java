package io.harness;

import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;
import software.wings.app.WingsPersistenceConfig;

public class DataGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class,
        WingsPersistenceConfig.class, ConnectorPersistenceConfig.class};
  }
}
