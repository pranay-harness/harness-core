package io.harness.event.metrics;

import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class MessagesMetricsGroupContext extends AutoMetricContext {
  public MessagesMetricsGroupContext(String accountId, String clusterId, String messageType) {
    put("accountId", accountId);
    put("clusterId", clusterId);
    put("messageType", messageType);
  }
}
