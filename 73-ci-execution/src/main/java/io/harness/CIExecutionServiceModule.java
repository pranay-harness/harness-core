package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;
import io.harness.registries.RegistryModule;

import java.util.Set;

public class CIExecutionServiceModule extends DependencyModule {
  @Override
  protected void configure() {
    // nothing to configure
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of(RegistryModule.getInstance());
  }
}