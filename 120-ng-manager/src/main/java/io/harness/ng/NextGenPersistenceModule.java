package io.harness.ng;

import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

public class NextGenPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return NextGenPersistenceConfig.class;
  }
}
