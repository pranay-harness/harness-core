package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.observer.AsyncInformObserver;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.queue.EventListenerObserver;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.PIPELINE)
public class MonitoringRedisEventObserver<T extends Message> implements EventListenerObserver<T>, AsyncInformObserver {
  public static String LISTENER_END_METRIC = "%s_queue_time";
  public static String LISTENER_START_METRIC = "%s_time_in_queue";

  private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Inject io.harness.metrics.service.api.MetricService metricService;
  @Inject MonitoringMetadataExtractorFactory monitoringMetadataExtractorFactory;
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
    if (monitoringMetadataExtractorFactory.getMetadataExtractor(message.getClass()) == null) {
      return;
    }
    MonitoringMetadataExtractor<T> monitoringMetadataExtractor =
        monitoringMetadataExtractorFactory.getMetadataExtractor(message.getClass());
    if (!pmsFeatureFlagHelper.isEnabled(
            monitoringMetadataExtractor.getAccountId(message), FeatureName.PIPELINE_MONITORING)) {
      return;
    }
    try (ThreadAutoLogContext autoLogContext = monitoringMetadataExtractor.metricContext(message)) {
      metricService.recordMetric(String.format(metricName, monitoringMetadataExtractor.getMetricPrefix(message)),
          System.currentTimeMillis() - monitoringMetadataExtractor.getCreatedAt(message));
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executor;
  }
}
