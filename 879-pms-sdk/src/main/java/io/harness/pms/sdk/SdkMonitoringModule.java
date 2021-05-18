package io.harness.pms.sdk;

import io.harness.metrics.modules.MetricsModule;

import com.google.inject.AbstractModule;

public class SdkMonitoringModule extends AbstractModule {
  static SdkMonitoringModule instance;

  public static SdkMonitoringModule getInstance() {
    if (instance == null) {
      instance = new SdkMonitoringModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(MetricsModule.getInstance());
  }
}
