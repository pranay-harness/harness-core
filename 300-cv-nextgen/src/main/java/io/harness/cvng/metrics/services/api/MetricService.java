package io.harness.cvng.metrics.services.api;

import io.harness.cvng.metrics.beans.CVNGMetricContext;

import java.io.IOException;

public interface MetricService {
  void initializeMetrics();
  String getRecordedMetricData() throws IOException;
  void recordMetric(String metricName, double value, CVNGMetricContext config);
}
