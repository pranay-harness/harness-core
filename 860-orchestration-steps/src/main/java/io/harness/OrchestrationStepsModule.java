package io.harness;

import io.harness.pms.sdk.core.registries.registrar.FacilitatorRegistrar;
import io.harness.pms.sdk.core.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.pms.sdk.core.registries.registrar.StepRegistrar;
import io.harness.registrars.OrchestrationStepsModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationStepsModuleFacilitatorRegistrar;
import io.harness.registrars.OrchestrationStepsModuleStepRegistrar;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.steps.barriers.service.BarrierServiceImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistryImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintServiceImpl;
import io.harness.steps.resourcerestraint.service.RestraintService;
import io.harness.steps.resourcerestraint.service.RestraintServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

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

    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(OrchestrationStepsModuleStepRegistrar.class.getName())
        .to(OrchestrationStepsModuleStepRegistrar.class);

    MapBinder<String, FacilitatorRegistrar> facilitatorRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    facilitatorRegistrarMapBinder.addBinding(OrchestrationStepsModuleFacilitatorRegistrar.class.getName())
        .to(OrchestrationStepsModuleFacilitatorRegistrar.class);

    MapBinder<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    orchestrationEventHandlerRegistrarMapBinder
        .addBinding(OrchestrationStepsModuleEventHandlerRegistrar.class.getName())
        .to(OrchestrationStepsModuleEventHandlerRegistrar.class);
  }
}
