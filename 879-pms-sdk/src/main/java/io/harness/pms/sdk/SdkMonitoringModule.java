/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
