package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.metrics.service.api.MetricService;
import io.harness.observer.AsyncInformObserver;
import io.harness.queue.Queuable;
import io.harness.queue.QueueListenerObserver;
import io.harness.queue.WithMonitoring;

import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.PIPELINE)
public class MonitoringQueueObserver<T extends Queuable> implements QueueListenerObserver<T>, AsyncInformObserver {
  public static String LISTENER_END_METRIC = "%s_queue_time";
  public static String LISTENER_START_METRIC = "%s_time_in_queue";

  private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Inject MetricService metricService;

  @Override
  public void onListenerEnd(T message) {
    if (WithMonitoring.class.isAssignableFrom(message.getClass())) {
      WithMonitoring monitoring = (WithMonitoring) message;
      try (ThreadAutoLogContext autoLogContext = monitoring.metricContext()) {
        metricService.recordMetric(String.format(LISTENER_END_METRIC, monitoring.getMetricPrefix()),
            System.currentTimeMillis() - monitoring.getCreatedAt());
      }
    }
  }

  @Override
  public void onListenerStart(T message) {
    if (WithMonitoring.class.isAssignableFrom(message.getClass())) {
      WithMonitoring monitoring = (WithMonitoring) message;
      try (ThreadAutoLogContext autoLogContext = monitoring.metricContext()) {
        metricService.recordMetric(String.format(LISTENER_START_METRIC, monitoring.getMetricPrefix()),
            System.currentTimeMillis() - monitoring.getCreatedAt());
      }
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }
}
