package io.harness;

import com.google.inject.AbstractModule;

import io.harness.walktree.registries.VisitorRegistryModule;

public class WalkTreeModule extends AbstractModule {
  private static volatile WalkTreeModule instance;

  public static WalkTreeModule getInstance() {
    if (instance == null) {
      instance = new WalkTreeModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(VisitorRegistryModule.getInstance());
  }
}
