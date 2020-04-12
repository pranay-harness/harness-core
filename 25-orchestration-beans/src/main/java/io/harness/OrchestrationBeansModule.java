package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;
import io.harness.registries.RegistryModule;

import java.util.Set;

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
    // No service to bind
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(RegistryModule.getInstance());
  }
}
