package io.harness;

import com.google.inject.AbstractModule;

public class AccessControlModule extends AbstractModule {
  private static AccessControlModule instance;

  public static AccessControlModule getInstance() {
    if (instance == null) {
      instance = new AccessControlModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {}
}
