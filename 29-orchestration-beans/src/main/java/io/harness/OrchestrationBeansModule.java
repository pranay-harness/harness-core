package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.registrars.OrchestrationBeansTimeoutRegistrar;
import io.harness.registries.OrchestrationRegistryModule;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.registries.registrar.TimeoutRegistrar;
import io.harness.testing.TestExecution;
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

    MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("Orchestration Alias Registrar Tests")
        .toInstance(OrchestrationAliasUtils::validateModule);
    MapBinder<String, TimeoutRegistrar> timeoutRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TimeoutRegistrar.class);
    timeoutRegistrarMapBinder.addBinding(OrchestrationBeansTimeoutRegistrar.class.getName())
        .to(OrchestrationBeansTimeoutRegistrar.class);
  }
}
