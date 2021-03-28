package io.harness.pms.interrupts;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.queue.Queuable;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "InterruptEventKeys")
@Entity(value = "interruptEventQueue", noClassnameStored = true)
@Document("interruptEventQueue")
@TypeAlias("interruptEvent")
@HarnessEntity(exportable = false)
public class InterruptEvent extends Queuable {
  @NonNull String interruptUuid;
  @NonNull NodeExecutionProto nodeExecution;
  @NonNull InterruptType interruptType;
  @Builder.Default String notifyId = generateUuid();

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>(AmbianceUtils.logContextMap(nodeExecution.getAmbiance()));
    logContext.put(InterruptEventKeys.interruptType, interruptType.name());
    logContext.put(InterruptEventKeys.interruptUuid, interruptUuid);
    logContext.put(InterruptEventKeys.notifyId, notifyId);
    return logContext;
  }
}
