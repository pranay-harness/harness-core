package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.metrics.service.api.MetricService;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.queue.EventListenerObserver;
import io.harness.queue.WithMonitoring;

import com.google.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.PIPELINE)
public class MonitoringEventObserver<T> implements EventListenerObserver<T>, AsyncInformObserver {
  public static String LISTENER_END_METRIC = "%s_queue_time";
  public static String LISTENER_START_METRIC = "%s_time_in_queue";

  private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Inject MetricService metricService;
  @Inject PmsFeatureFlagHelper pmsFeatureFlagHelper;

  @Override
  public void onListenerEnd(T message) {
    sendMetric(message, LISTENER_END_METRIC);
  }

  @Override
  public void onListenerStart(T message) {
    sendMetric(message, LISTENER_START_METRIC);
  }

  private void sendMetric(T message, String metricName) {
    if (WithMonitoring.class.isAssignableFrom(message.getClass())) {
      WithMonitoring monitoring = (WithMonitoring) message;
      if (!pmsFeatureFlagHelper.isEnabled(monitoring.getAccountId(), FeatureName.PIPELINE_MONITORING)) {
        return;
      }
      try (ThreadAutoLogContext autoLogContext = monitoring.metricContext()) {
        metricService.recordMetric(String.format(metricName, monitoring.getMetricPrefix()),
            System.currentTimeMillis() - monitoring.getCreatedAt());
      }
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }
}
