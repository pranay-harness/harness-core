package io.harness.executionplan.rule;

import io.harness.testlib.PersistenceTestModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import software.wings.app.WingsPersistenceConfig;

public class CIExecutionPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return WingsPersistenceConfig.class;
  }
}
