package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.CreatedByAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAccess;
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
public class Interrupt implements PersistentEntity, UuidAccess, CreatedAtAccess, UpdatedAtAccess, CreatedByAccess {
  @Id @NotNull String uuid;
  @NonNull ExecutionInterruptType type;
  @NonNull String planExecutionId;
  @NonFinal @Setter String nodeExecutionId;
  @NonNull EmbeddedUser createdBy;
  long lastUpdatedAt;
  long createdAt;
  boolean seized;
}
