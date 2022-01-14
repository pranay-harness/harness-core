package io.harness.monitoring;

import com.google.protobuf.Message;
import java.util.Map;

public interface EventMonitoringService {
  <T extends Message> void sendMetric(
      String metricName, MonitoringInfo monitoringInfo, Map<String, String> metadataMap);
}
