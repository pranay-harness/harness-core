package io.harness.gitsync;

import io.harness.govern.DependencyModule;

import java.util.Set;

public class GitSyncModule extends DependencyModule {
  private static volatile GitSyncModule instance;

  public static GitSyncModule getInstance() {
    if (instance == null) {
      instance = new GitSyncModule();
    }
    return instance;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }

  @Override
  protected void configure() {}
}
