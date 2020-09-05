package io.harness;

import com.google.inject.TypeLiteral;

import io.harness.springdata.SpringPersistenceConfig;
import io.harness.steps.resourcerestraint.service.RestraintService;
import io.harness.steps.resourcerestraint.service.RestraintTestService;
import io.harness.testlib.PersistenceTestModule;

public class OrchestrationStepsPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    bind(new TypeLiteral<RestraintService>() {}).to(RestraintTestService.class);
    return new Class[] {TimeoutEnginePersistenceConfig.class, OrchestrationPersistenceConfig.class,
        OrchestrationStepsPersistenceConfig.class};
  }
}
