package software.wings.rules;

import io.harness.OrchestrationPersistenceConfig;
import io.harness.ng.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;
import software.wings.app.WingsPersistenceConfig;

public class WingsPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {OrchestrationPersistenceConfig.class, WingsPersistenceConfig.class};
  }
}
