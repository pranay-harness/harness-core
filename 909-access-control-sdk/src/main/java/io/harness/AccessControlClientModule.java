package io.harness;

import com.google.inject.AbstractModule;

public class AccessControlClientModule extends AbstractModule {
  private static AccessControlClientModule instance;

  public static AccessControlClientModule getInstance() {
    if (instance == null) {
      instance = new AccessControlClientModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
