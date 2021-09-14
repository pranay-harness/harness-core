/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.events.timeseries.data;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CostEventData {
  String accountId;
  String settingId;
  String clusterId;
  String clusterType;
  String instanceId;
  String instanceType;
  String appId;
  String serviceId;
  String envId;
  String cloudProviderId;
  String deploymentId;
  String cloudProvider;
  String eventDescription;
  String costEventType;
  String costEventSource;
  String namespace;
  String workloadName;
  String workloadType;
  String cloudServiceName;
  String taskId;
  String launchType;

  BigDecimal billingAmount;

  long startTimestamp;

  String oldYamlRef;
  String newYamlRef;

  BigDecimal costChangePercent;
}
