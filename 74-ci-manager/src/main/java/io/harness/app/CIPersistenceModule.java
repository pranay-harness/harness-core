package io.harness.app;

import io.harness.ng.PersistenceModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import software.wings.app.WingsPersistenceConfig;

public class CIPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return WingsPersistenceConfig.class;
  }
}
