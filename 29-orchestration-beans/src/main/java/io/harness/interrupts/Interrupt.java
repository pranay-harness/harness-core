package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@Entity(value = "interrupts")
@Document(value = "interrupts")
@TypeAlias(value = "interrupts")
@FieldNameConstants(innerTypeName = "InterruptKeys")
public class Interrupt implements PersistentEntity, UuidAccess, CreatedByAccess {
  public enum State { REGISTERED, PROCESSING, PROCESSED_SUCCESSFULLY, PROCESSED_UNSUCCESSFULLY, DISCARDED }

  @Wither @Id @org.mongodb.morphia.annotations.Id @NotNull String uuid;
  @NonNull ExecutionInterruptType type;
  @NonNull String planExecutionId;
  String nodeExecutionId;
  StepParameters parameters;
  EmbeddedUser createdBy;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @CreatedDate Long createdAt;
  @NonFinal @Setter @Builder.Default State state = State.REGISTERED;
  @Wither @Version Long version;

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }
  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put(InterruptKeys.planExecutionId, planExecutionId);
    logContext.put(InterruptKeys.type, type.name());
    logContext.put(InterruptKeys.nodeExecutionId, nodeExecutionId);
    return logContext;
  }
}
