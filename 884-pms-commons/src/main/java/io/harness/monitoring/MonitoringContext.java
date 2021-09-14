/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@TypeAlias("MonitoringContext")
public class MonitoringContext implements GlobalContextData {
  public static final String IS_MONITORING_ENABLED = "IS_MONITORING_ENABLED";
  boolean isMonitoringEnabled;

  @Override
  public String getKey() {
    return IS_MONITORING_ENABLED;
  }
}
