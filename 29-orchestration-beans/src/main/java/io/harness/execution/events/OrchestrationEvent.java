package io.harness.execution.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.ambiance.Ambiance;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.queue.Queuable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "OrchestrationEventKeys")
@Entity(value = "orchestrationEventQueue")
@Document("orchestrationEventQueue")
@HarnessEntity(exportable = false)
public class OrchestrationEvent extends Queuable {
  Ambiance ambiance;
  OrchestrationEventType eventType;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = ambiance.logContextMap();
    logContext.put(OrchestrationEventKeys.eventType, eventType.getType());
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }
}
