package io.harness.version;

import io.harness.govern.DependencyModule;

import java.util.Set;

public class VersionModule extends DependencyModule {
  private static VersionModule instance;

  public static VersionModule getInstance() {
    if (instance == null) {
      instance = new VersionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(VersionInfoManager.class).toInstance(new VersionInfoManager());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
