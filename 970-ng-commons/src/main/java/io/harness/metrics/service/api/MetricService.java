package io.harness.metrics.service.api;

public interface MetricService {
  void recordMetric(String metricName, double value);
}
