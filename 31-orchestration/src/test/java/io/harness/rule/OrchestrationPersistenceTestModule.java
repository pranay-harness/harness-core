package io.harness.rule;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

public class OrchestrationPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return OrchestrationPersistenceConfig.class;
  }
}
