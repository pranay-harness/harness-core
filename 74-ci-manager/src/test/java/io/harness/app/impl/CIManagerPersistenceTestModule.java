package io.harness.app.impl;

import io.harness.testlib.PersistenceTestModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import software.wings.app.WingsPersistenceConfig;

public class CIManagerPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return WingsPersistenceConfig.class;
  }
}
