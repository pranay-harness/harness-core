package io.harness.perpetualtask.internal;

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
public class PerpetualTaskRecord
    implements PersistentEntity, UuidAware, PersistentRegularIterable, CreatedAtAware, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  PerpetualTaskType perpetualTaskType;
  PerpetualTaskClientContext clientContext;
  long intervalSeconds;
  long timeoutMillis;
  String delegateId;
  long lastHeartbeat;
  @Indexed Long nextIteration;
  long createdAt;
  long lastUpdatedAt;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }
}
