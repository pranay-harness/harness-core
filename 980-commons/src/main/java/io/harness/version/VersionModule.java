package io.harness.version;

import com.google.inject.AbstractModule;

public class VersionModule extends AbstractModule {
  private static VersionModule instance;

  private VersionModule() {}

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
}
