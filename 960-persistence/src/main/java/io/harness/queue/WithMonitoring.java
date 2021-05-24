package io.harness.queue;

import io.harness.metrics.ThreadAutoLogContext;

public interface WithMonitoring {
  ThreadAutoLogContext metricContext();

  String getMetricPrefix();

  Long getCreatedAt();
}
