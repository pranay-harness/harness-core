package io.harness;

import io.harness.pms.sdk.core.registries.PmsSdkCoreRegistryModule;
import io.harness.pms.sdk.core.registries.registrar.AdviserRegistrar;
import io.harness.pms.sdk.core.registries.registrar.FacilitatorRegistrar;
import io.harness.pms.sdk.core.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.pms.sdk.core.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.pms.sdk.core.registries.registrar.ResolverRegistrar;
import io.harness.pms.sdk.core.registries.registrar.StepRegistrar;
import io.harness.pms.sdk.core.registries.registrar.local.PmsSdkCoreAdviserRegistrar;
import io.harness.pms.sdk.core.registries.registrar.local.PmsSdkCoreFacilitatorRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class PmsSdkCoreModule extends AbstractModule {
  private static PmsSdkCoreModule instance;

  public static PmsSdkCoreModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkCoreModule();
    }
    return instance;
  }

  private PmsSdkCoreModule() {}

  @Override
  protected void configure() {
    install(PmsSdkCoreRegistryModule.getInstance());
    MapBinder<String, FacilitatorRegistrar> facilitatorRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    facilitatorRegistrarMapBinder.addBinding(PmsSdkCoreFacilitatorRegistrar.class.getName())
        .to(PmsSdkCoreFacilitatorRegistrar.class);

    MapBinder<String, AdviserRegistrar> adviserRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    adviserRegistrarMapBinder.addBinding(PmsSdkCoreAdviserRegistrar.class.getName())
        .to(PmsSdkCoreAdviserRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);

    MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
  }
}
