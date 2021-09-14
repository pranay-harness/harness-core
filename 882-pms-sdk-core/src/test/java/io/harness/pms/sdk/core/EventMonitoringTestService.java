/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.core;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.monitoring.EventMonitoringService;
import io.harness.monitoring.MonitoringInfo;

import com.google.protobuf.Message;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class EventMonitoringTestService implements EventMonitoringService {
  @Override
  public <T extends Message> void sendMetric(
      String metricName, MonitoringInfo monitoringInfo, Map<String, String> metadataMap) {}
}
