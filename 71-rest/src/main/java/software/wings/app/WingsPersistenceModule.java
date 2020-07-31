package software.wings.app;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class WingsPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {
        OrchestrationPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class, WingsPersistenceConfig.class};
  }
}
