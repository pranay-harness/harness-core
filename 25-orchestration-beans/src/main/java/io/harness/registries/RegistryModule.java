package io.harness.registries;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;

import java.util.Set;

public class RegistryModule extends DependencyModule {
  private static RegistryModule instance;

  public static RegistryModule getInstance() {
    if (instance == null) {
      instance = new RegistryModule();
    }
    return instance;
  }
  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }

  @Override
  protected void configure() {}
}
