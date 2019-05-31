package io.harness.migrator.app;

import com.google.common.collect.ImmutableSet;

import io.harness.govern.DependencyModule;
import io.harness.time.TimeModule;

import java.util.Set;

public class MigratorModule extends DependencyModule {
  @Override
  protected void configure() {}

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(TimeModule.getInstance());
  }
}
