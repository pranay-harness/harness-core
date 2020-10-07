package io.harness;

import io.harness.springdata.PersistenceModule;
import io.harness.springdata.SpringPersistenceConfig;

public class PipelineServicePersistenceTestModule extends PersistenceModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        PipelineServicePersistenceConfig.class};
  }
}
