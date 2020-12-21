package io.harness.pms.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.queue.Queuable;

import java.util.HashMap;
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
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "NodeExecutionEventKeys")
@Entity(value = "nodeExecutionEventQueue")
@Document("nodeExecutionEventQueue")
@TypeAlias("nodeExecutionEvent")
@HarnessEntity(exportable = false)
@Builder
public class NodeExecutionEvent extends Queuable {
  NodeExecutionProto nodeExecution;
  NodeExecutionEventType eventType;
  NodeExecutionEventData eventData;
  @Builder.Default String notifyId = generateUuid();

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put(NodeExecutionEventKeys.eventType, eventType.name());
    logContext.putAll(AmbianceUtils.logContextMap(nodeExecution.getAmbiance()));
    logContext.put(NodeExecutionEventKeys.notifyId, notifyId);
    return logContext;
  }
}
