package io.harness.queue;

import io.harness.metrics.MetricContext;
import io.harness.metrics.service.api.MetricService;

public abstract class QueueListenerWithMonitoring<T extends QueuableWithMonitoring> extends QueueListener<T> {
  private final MetricService metricService;

  public QueueListenerWithMonitoring(MetricService metricService, QueueConsumer<T> queueConsumer, boolean primaryOnly) {
    super(queueConsumer, primaryOnly);
    this.metricService = metricService;
  }

  @Override
  public void onMessage(T message) {
    try {
      onMessageInternal(message);
    } finally {
      recordMetrics(message);
    }
  }

  public abstract void onMessageInternal(T message);

  void recordMetrics(T message) {
    try (MetricContext metricContext = message.metricContext()) {
      metricService.recordMetric("node_execution_queue_time", System.currentTimeMillis() - message.getCreatedAt());
    }
  }
}
