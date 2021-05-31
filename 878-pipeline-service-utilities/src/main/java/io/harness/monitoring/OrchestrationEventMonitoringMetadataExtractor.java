package io.harness.monitoring;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.pms.contracts.execution.events.OrchestrationEvent;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.serializer.ProtoUtils;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventMonitoringMetadataExtractor implements MonitoringMetadataExtractor<OrchestrationEvent> {
  @Override
  public ThreadAutoLogContext metricContext(OrchestrationEvent orchestrationEvent) {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(orchestrationEvent.getAmbiance()));
    logContext.put("eventType", orchestrationEvent.getEventType().name());
    logContext.put("module", orchestrationEvent.getNodeExecution().getNode().getServiceName());
    logContext.put("pipelineIdentifier", orchestrationEvent.getAmbiance().getMetadata().getPipelineIdentifier());
    return new ThreadAutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public String getMetricPrefix(OrchestrationEvent message) {
    return "orchestration_event";
  }

  public Class<OrchestrationEvent> getType() {
    return OrchestrationEvent.class;
  }

  public Long getCreatedAt(OrchestrationEvent event) {
    return ProtoUtils.timestampToUnixMillis(event.getCreatedAt());
  }
}
