package io.harness.execution.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "OrchestrationEventKeys")
public class OrchestrationEvent {
  Ambiance ambiance;
  OrchestrationEventType eventType;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = ambiance.logContextMap();
    logContext.put(OrchestrationEventKeys.eventType, eventType.getType());
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }
}
