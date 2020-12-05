package io.harness;

import io.harness.orchestration.OrchestrationPersistenceModule;
import io.harness.registrars.OrchestrationBeansFieldRegistrar;
import io.harness.registrars.OrchestrationBeansTimeoutRegistrar;
import io.harness.registries.OrchestrationRegistryModule;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.registries.registrar.TimeoutRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrchestrationBeansModule extends AbstractModule {
  private static OrchestrationBeansModule instance;

  public static OrchestrationBeansModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationBeansModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(TimeoutEngineModule.getInstance());
    install(OrchestrationRegistryModule.getInstance());
    install(OrchestrationPersistenceModule.getInstance());
    install(PmsSdkCoreModule.getInstance());

    MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    MapBinder<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationFieldRegistrar.class);
    orchestrationFieldRegistrarMapBinder.addBinding(OrchestrationBeansFieldRegistrar.class.getName())
        .to(OrchestrationBeansFieldRegistrar.class);
    MapBinder<String, TimeoutRegistrar> timeoutRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TimeoutRegistrar.class);
    timeoutRegistrarMapBinder.addBinding(OrchestrationBeansTimeoutRegistrar.class.getName())
        .to(OrchestrationBeansTimeoutRegistrar.class);
  }
}
