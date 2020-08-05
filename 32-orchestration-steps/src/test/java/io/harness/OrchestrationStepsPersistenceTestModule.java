package io.harness;

import com.google.inject.TypeLiteral;

import io.harness.springdata.SpringPersistenceConfig;
import io.harness.steps.resourcerestraint.RestraintTestService;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.service.RestraintService;
import io.harness.testlib.PersistenceTestModule;

public class OrchestrationStepsPersistenceTestModule extends PersistenceTestModule {
  @Override
  protected Class<? extends SpringPersistenceConfig>[] getConfigClasses() {
    bind(new TypeLiteral<RestraintService<? extends ResourceRestraint>>() {}).to(RestraintTestService.class);
    return new Class[] {OrchestrationPersistenceConfig.class, OrchestrationStepsPersistenceConfig.class};
  }
}
