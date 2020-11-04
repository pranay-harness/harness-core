package io.harness.app;

import io.harness.CIExecutionPersistenceConfig;
import io.harness.NGPipelinePersistenceConfig;
import io.harness.OrchestrationPersistenceConfig;
import io.harness.OrchestrationStepsPersistenceConfig;
import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;
import software.wings.app.WingsPersistenceConfig;

public class CIPersistenceModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        WingsPersistenceConfig.class, CIManagerPersistenceConfig.class, CIExecutionPersistenceConfig.class,
        CIExecutionPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class,
        NGPipelinePersistenceConfig.class};
  }
}
