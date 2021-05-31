package io.harness.monitoring;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.ThreadAutoLogContext;

import com.google.protobuf.Message;

@OwnedBy(HarnessTeam.PIPELINE)
public interface MonitoringMetadataExtractor<T extends Message> {
  ThreadAutoLogContext metricContext(T message);
  String getMetricPrefix(T message);
  Class<T> getType();
  Long getCreatedAt(T message);
}
