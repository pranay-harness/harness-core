package io.harness.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.DependencyModule;

import java.util.Set;

@OwnedBy(CDC)
public abstract class RegistryModule extends DependencyModule {
  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.of();
  }

  @Override
  protected void configure() {
    // Nothing to configure
  }
}
