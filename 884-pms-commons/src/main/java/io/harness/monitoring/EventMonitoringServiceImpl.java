package io.harness.monitoring;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.service.api.MetricService;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class EventMonitoringServiceImpl implements EventMonitoringService {
  @Inject MetricService metricService;

  // Todo: Introduce sampling
  public <T extends Message> void sendMetric(
      String metricName, MonitoringInfo monitoringInfo, Map<String, String> metadataMap) {
    try {
      if (!Objects.equals(metadataMap.get(PIPELINE_MONITORING_ENABLED), "true")) {
        return;
      }
      metricService.recordMetric(String.format(metricName, monitoringInfo.getMetricPrefix()),
          System.currentTimeMillis() - monitoringInfo.getCreatedAt());
    } catch (Exception ex) {
      // Ignore the error
    }
  }
}
