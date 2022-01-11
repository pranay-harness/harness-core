/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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

  public PmsMetricContextGuard(Map<String, String> contextMap) {
    super(contextMap);
  }
}
