package io.harness;

import com.google.common.collect.ImmutableSet;
import com.google.inject.multibindings.MapBinder;

import io.harness.govern.DependencyModule;
import io.harness.registries.RegistryModule;
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
