package io.harness;

import com.google.inject.AbstractModule;

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
    // Nothing to register.
  }
}
