package io.harness.pms.sdk.core;

import com.google.inject.AbstractModule;

public class PmsSdkCoreModule extends AbstractModule {
  private static PmsSdkCoreModule instance;
  private final PmsSdkCoreConfig config;

  public static PmsSdkCoreModule getInstance(PmsSdkCoreConfig config) {
    if (instance == null) {
      instance = new PmsSdkCoreModule(config);
    }
    return instance;
  }

  private PmsSdkCoreModule(PmsSdkCoreConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    if (config.getSdkDeployMode().isNonLocal()) {
      install(PmsSdkGrpcModule.getInstance(config));
    } else {
      install(PmsSdkDummyGrpcModule.getInstance());
    }
  }
}
