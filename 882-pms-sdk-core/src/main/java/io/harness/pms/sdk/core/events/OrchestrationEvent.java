package io.harness.pms.sdk.core.events;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.queue.Queuable;

import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "OrchestrationEventKeys")
@Entity(value = "orchestrationEventQueue")
@Document("orchestrationEventQueue")
@HarnessEntity(exportable = false)
@TypeAlias("orchestrationEvent")
public class OrchestrationEvent extends Queuable {
  Ambiance ambiance;
  OrchestrationEventType eventType;

  public AutoLogContext autoLogContext() {
    Map<String, String> logContext = AmbianceUtils.logContextMap(ambiance);
    logContext.put(OrchestrationEventKeys.eventType, eventType.name());
    return new AutoLogContext(logContext, OVERRIDE_NESTS);
  }
}
