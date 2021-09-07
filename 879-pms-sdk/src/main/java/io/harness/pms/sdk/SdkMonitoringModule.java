package io.harness.pms.sdk;

import io.harness.metrics.modules.MetricsModule;
import io.harness.monitoring.EventMonitoringService;
import io.harness.monitoring.EventMonitoringServiceImpl;

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
    install(new MetricsModule());
    bind(EventMonitoringService.class).to(EventMonitoringServiceImpl.class);
  }
}
