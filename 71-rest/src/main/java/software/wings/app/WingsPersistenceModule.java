package software.wings.app;

import io.harness.ng.PersistenceModule;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;

public class WingsPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends AbstractMongoConfiguration> getConfigClass() {
    return WingsPersistenceConfig.class;
  }
}
