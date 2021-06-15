package io.harness.monitoring;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.observer.AsyncInformObserver;
import io.harness.queue.EventListenerObserver;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OwnedBy(HarnessTeam.PIPELINE)
public class MonitoringRedisEventObserver<T extends Message> implements EventListenerObserver<T>, AsyncInformObserver {
  public static String LISTENER_END_METRIC = "%s_queue_time";
  public static String LISTENER_START_METRIC = "%s_time_in_queue";

  private static final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Inject io.harness.metrics.service.api.MetricService metricService;
  @Inject MonitoringMetadataExtractorFactory monitoringMetadataExtractorFactory;

  @Override
  public void onListenerEnd(T message, Map<String, String> metadataMap) {
    sendMetric(message, LISTENER_END_METRIC, metadataMap);
  }

  @Override
  public void onListenerStart(T message, Map<String, String> metadataMap) {
    sendMetric(message, LISTENER_START_METRIC, metadataMap);
  }

  private void sendMetric(T message, String metricName, Map<String, String> metadataMap) {
    if (monitoringMetadataExtractorFactory.getMetadataExtractor(message.getClass()) == null) {
      return;
    }
    MonitoringMetadataExtractor<T> monitoringMetadataExtractor =
        monitoringMetadataExtractorFactory.getMetadataExtractor(message.getClass());
    if (!Objects.equals(metadataMap.get(PIPELINE_MONITORING_ENABLED), "true")) {
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
