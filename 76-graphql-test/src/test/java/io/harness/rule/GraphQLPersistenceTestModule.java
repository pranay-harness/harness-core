package io.harness.rule;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.connector.ConnectorPersistenceConfig;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import software.wings.app.WingsPersistenceConfig;

public class GraphQLPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {
        OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class, ConnectorPersistenceConfig.class};
  }
}
