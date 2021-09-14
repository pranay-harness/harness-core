/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.timeseries.data;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CE)
@Value
@Builder
public class K8sGranularUtilizationData {
  private String accountId;
  // 'instanceId' will be depricated soon for 'actualInstanceId'
  private String instanceId;
  private String actualInstanceId;
  private String instanceType;
  private String clusterId;
  private String settingId;
  private double cpu;
  private double memory;
  private double maxCpu;
  private double maxMemory;
  private double storageUsageValue;
  private double storageRequestValue;
  private long endTimestamp;
  private long startTimestamp;
}
