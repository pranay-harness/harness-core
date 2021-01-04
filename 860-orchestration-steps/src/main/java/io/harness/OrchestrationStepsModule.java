package io.harness;

import io.harness.steps.barriers.service.BarrierService;
import io.harness.steps.barriers.service.BarrierServiceImpl;
import io.harness.steps.resourcerestraint.service.*;

import com.google.inject.AbstractModule;

public class OrchestrationStepsModule extends AbstractModule {
  private static OrchestrationStepsModule instance;

  public static OrchestrationStepsModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationStepsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(BarrierService.class).to(BarrierServiceImpl.class);
    bind(RestraintService.class).to(RestraintServiceImpl.class);
    bind(ResourceRestraintService.class).to(ResourceRestraintServiceImpl.class);
    bind(ResourceRestraintRegistry.class).to(ResourceRestraintRegistryImpl.class);
  }
}
