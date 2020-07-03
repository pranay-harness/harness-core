package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.multibindings.MapBinder;

import io.harness.govern.DependencyModule;
import io.harness.registries.RegistryModule;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.testing.TestExecution;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class OrchestrationBeansModule extends DependencyModule {
  private static OrchestrationBeansModule instance;

  public static OrchestrationBeansModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationBeansModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("Orchestration Alias Registrar Tests")
        .toInstance(OrchestrationAliasUtils::validateModule);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(RegistryModule.getInstance());
  }
}
