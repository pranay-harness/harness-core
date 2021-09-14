/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UtilizationData {
  private double maxCpuUtilization;
  private double maxMemoryUtilization;
  private double avgCpuUtilization;
  private double avgMemoryUtilization;
  private double maxCpuUtilizationValue;
  private double maxMemoryUtilizationValue;
  private double avgCpuUtilizationValue;
  private double avgMemoryUtilizationValue;

  private double avgStorageCapacityValue;
  private double avgStorageUsageValue;
  private double avgStorageRequestValue;

  private double maxStorageUsageValue;
  private double maxStorageRequestValue;
}
