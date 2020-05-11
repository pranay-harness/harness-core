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
import lombok.Value;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@Entity(value = "interrupts", noClassnameStored = true)
public class Interrupt implements PersistentEntity, UuidAccess, CreatedAtAccess, UpdatedAtAccess, CreatedByAccess {
  @Id @NotNull String uuid;
  @NotNull ExecutionInterruptType type;
  @NotNull String planExecutionId;
  String nodeExecutionId;
  EmbeddedUser createdBy;
  long lastUpdatedAt;
  long createdAt;

  @NonFinal boolean seized;

  public void setSeized(boolean seized) {
    this.seized = seized;
  }
}
