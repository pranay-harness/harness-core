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
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActualIdleCostData {
  String accountId;
  String clusterId;
  String instanceId;
  String parentInstanceId;
  double cost;
  double cpuCost;
  double memoryCost;
  double idleCost;
  double cpuIdleCost;
  double memoryIdleCost;
  double systemCost;
  double cpuSystemCost;
  double memorySystemCost;
  long startTime;
  long endTime;
}
