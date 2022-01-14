package io.harness.batch.processing.svcmetrics;

import io.harness.metrics.service.api.MetricService;
import io.harness.metrics.service.api.MetricsPublisher;

import com.google.inject.Inject;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

// This class is a singleton. @see BatchProcessingModule.configure().
@Slf4j
public class BatchProcessingMetricsPublisher implements MetricsPublisher {
  @Inject private MetricService metricService;

  @Override
  public void recordMetrics() {
    recordDummyMetric();
  }

  // Test whether metrics collection is working.
  private void recordDummyMetric() {
    Random rng = new Random();
    int n = rng.nextInt(10);
    try (BatchJobContext _ = new BatchJobContext("dummyAccountID", "dummyJobType")) {
      for (int i = 0; i < n; i++) {
        metricService.incCounter(BatchProcessingMetricName.DUMMY_METRIC);
      }
    }
  }
}
