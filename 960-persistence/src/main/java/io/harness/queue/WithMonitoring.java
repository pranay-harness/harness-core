package io.harness.queue;

import io.harness.metrics.ThreadAutoLogContext;
import io.harness.persistence.CreatedAtAware;

public interface WithMonitoring extends CreatedAtAware {
  ThreadAutoLogContext metricContext();

  String getMetricPrefix();
}
