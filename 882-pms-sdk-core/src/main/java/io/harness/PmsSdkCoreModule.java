package io.harness;

import io.harness.registrars.PmsSdkCoreAdviserRegistrar;
import io.harness.registrars.PmsSdkCoreFacilitatorRegistrar;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;

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
    MapBinder<String, FacilitatorRegistrar> facilitatorRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    facilitatorRegistrarMapBinder.addBinding(PmsSdkCoreFacilitatorRegistrar.class.getName())
        .to(PmsSdkCoreFacilitatorRegistrar.class);

    MapBinder<String, AdviserRegistrar> adviserRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    adviserRegistrarMapBinder.addBinding(PmsSdkCoreAdviserRegistrar.class.getName())
        .to(PmsSdkCoreAdviserRegistrar.class);
  }
}
