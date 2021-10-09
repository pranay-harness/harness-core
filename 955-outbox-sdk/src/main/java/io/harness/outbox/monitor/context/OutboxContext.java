package io.harness.outbox.monitor.context;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.AutoMetricContext;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(PL)
public class OutboxContext extends AutoMetricContext {
  public OutboxContext(String serviceId, String eventType) {
    put("serviceId", serviceId);
    put("eventType", eventType);
  }
}
