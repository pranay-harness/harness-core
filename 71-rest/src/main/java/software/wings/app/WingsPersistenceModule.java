package software.wings.app;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.ng.PersistenceModule;
import io.harness.ng.SpringPersistenceConfig;

public class WingsPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class};
  }
}
