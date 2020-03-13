package io.harness.perpetualtask.internal;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "PerpetualTaskRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "perpetualTask", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class PerpetualTaskRecord
    implements PersistentEntity, UuidAware, PersistentRegularIterable, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  @Indexed String accountId;
  PerpetualTaskType perpetualTaskType;
  PerpetualTaskClientContext clientContext;
  long intervalSeconds;
  long timeoutMillis;
  String delegateId;
  long lastHeartbeat;

  @Indexed Long assignerIteration;
  @Indexed Long resetterIteration;

  long createdAt;
  long lastUpdatedAt;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (PerpetualTaskRecordKeys.assignerIteration.equals(fieldName)) {
      return this.assignerIteration;
    }

    if (PerpetualTaskRecordKeys.resetterIteration.equals(fieldName)) {
      return this.resetterIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (PerpetualTaskRecordKeys.assignerIteration.equals(fieldName)) {
      this.assignerIteration = nextIteration;
      return;
    }
    if (PerpetualTaskRecordKeys.resetterIteration.equals(fieldName)) {
      this.resetterIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}
