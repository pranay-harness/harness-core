package io.harness.rule;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import software.wings.app.WingsPersistenceConfig;

public class GraphQLPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class, WingsPersistenceConfig.class, ConnectorPersistenceConfig.class};
  }
}
