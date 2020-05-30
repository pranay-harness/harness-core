package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAccess;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAccess;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@Entity(value = "interrupts")
@FieldNameConstants(innerTypeName = "InterruptKeys")
public class Interrupt implements PersistentEntity, UuidAccess, CreatedAtAccess, CreatedAtAware, UpdatedAtAccess,
                                  UpdatedAtAware, CreatedByAccess {
  public enum State { REGISTERED, PROCESSING, PROCESSED_SUCCESSFULLY, PROCESSED_UNSUCCESSFULLY, DISCARDED }

  @Id @NotNull String uuid;
  @NonNull ExecutionInterruptType type;
  @NonNull String planExecutionId;
  String nodeExecutionId;
  @NonNull EmbeddedUser createdBy;
  @NonFinal @Setter long lastUpdatedAt;
  @NonFinal @Setter long createdAt;
  @NonFinal @Setter @Builder.Default State state = State.REGISTERED;
}
