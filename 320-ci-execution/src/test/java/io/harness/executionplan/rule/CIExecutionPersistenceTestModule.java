package io.harness.executionplan.rule;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;
import software.wings.app.WingsPersistenceConfig;

public class CIExecutionPersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        WingsPersistenceConfig.class, ConnectorPersistenceConfig.class, CIExecutionPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class};
  }
}
