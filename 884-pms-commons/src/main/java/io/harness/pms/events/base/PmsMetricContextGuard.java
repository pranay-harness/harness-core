/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events.base;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.manage.GlobalContextManager;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.monitoring.MonitoringContext;

import java.util.Map;
import java.util.Objects;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsMetricContextGuard extends ThreadAutoLogContext {
  public PmsMetricContextGuard(Map<String, String> metadataMap, Map<String, String> contextMap) {
    super(contextMap);
    GlobalContextManager.upsertGlobalContextRecord(
        MonitoringContext.builder()
            .isMonitoringEnabled(Objects.equals(metadataMap.getOrDefault(PIPELINE_MONITORING_ENABLED, "false"), "true"))
            .build());
  }
}
