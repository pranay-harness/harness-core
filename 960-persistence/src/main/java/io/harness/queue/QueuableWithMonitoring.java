package io.harness.queue;

import io.harness.metrics.MetricContext;

import lombok.Getter;
import lombok.Setter;

public abstract class QueuableWithMonitoring extends Queuable {
  @Getter @Setter private long createdAt;

  public abstract MetricContext metricContext();

  public abstract String getMetricPrefix();
}
