package io.harness.batch.processing.billing.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UtilizationData {
  private double cpuUtilization;
  private double memoryUtilization;
}
