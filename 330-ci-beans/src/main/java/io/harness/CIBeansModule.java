package io.harness;

import com.google.inject.AbstractModule;

public class CIBeansModule extends AbstractModule {
  private static CIBeansModule instance;

  public static CIBeansModule getInstance() {
    if (instance == null) {
      instance = new CIBeansModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
