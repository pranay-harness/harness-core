package io.harness;

import io.harness.ng.PersistenceModule;
import io.harness.ng.SpringMongoConfig;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

public class DataGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return SpringMongoConfig.class;
  }
}
