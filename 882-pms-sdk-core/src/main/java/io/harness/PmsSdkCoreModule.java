package io.harness;

import com.google.inject.AbstractModule;

public class PmsSdkCoreModule extends AbstractModule {
  private static PmsSdkCoreModule instance;

  public static PmsSdkCoreModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkCoreModule();
    }
    return instance;
  }

  private PmsSdkCoreModule() {}

  @Override
  protected void configure() {}
}
