package io.harness.queue;

import io.harness.govern.DependencyModule;

import java.util.Set;

public class QueueModule extends DependencyModule {
  private static QueueModule instance;

  public static QueueModule getInstance() {
    if (instance == null) {
      instance = new QueueModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
