package io.harness.ng.core;

import io.harness.ng.NextGenPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

public class CorePersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return NextGenPersistenceConfig.class;
  }
}
