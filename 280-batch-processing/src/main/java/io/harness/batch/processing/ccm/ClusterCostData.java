/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.ccm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterCostData {
  double totalCost;
  double totalSystemCost;
  double utilizedCost;
  double cpuTotalCost;
  double cpuSystemCost;
  double cpuUtilizedCost;
  double memoryTotalCost;
  double memorySystemCost;
  double memoryUtilizedCost;
  long startTime;
  long endTime;
  String accountId;
  String billingAccountId;
  String clusterName;
  String settingId;
  String region;
  String cloudProvider;
  String workloadType;
  String clusterType;
}
