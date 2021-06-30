package io.harness.monitoring;

import io.harness.metrics.ThreadAutoLogContext;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitoringInfo {
  String metricPrefix;
  Long createdAt;
  ThreadAutoLogContext metricContext;
  String accountId;
}
