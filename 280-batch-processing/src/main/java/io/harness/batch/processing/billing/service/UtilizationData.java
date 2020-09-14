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
}
