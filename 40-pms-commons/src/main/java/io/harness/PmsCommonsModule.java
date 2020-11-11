package io.harness;

import com.google.inject.AbstractModule;

public class PmsCommonsModule extends AbstractModule {
  private static PmsCommonsModule instance;

  public static PmsCommonsModule getInstance() {
    if (instance == null) {
      instance = new PmsCommonsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
