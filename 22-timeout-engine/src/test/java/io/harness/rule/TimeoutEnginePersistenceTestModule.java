package io.harness.rule;

import io.harness.TimeoutEnginePersistenceConfig;
import io.harness.springdata.SpringPersistenceConfig;
import io.harness.testlib.PersistenceTestModule;

public class TimeoutEnginePersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    return new Class[] {TimeoutEnginePersistenceConfig.class};
  }
}
