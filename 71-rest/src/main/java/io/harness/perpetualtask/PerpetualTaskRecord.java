package io.harness.perpetualtask;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "PerpetualTaskRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "perpetualTask", noClassnameStored = true)
public class PerpetualTaskRecord implements PersistentEntity, CreatedAtAware, UpdatedAtAware, UuidAware {
  @Id String uuid;
  String accountId;
  PerpetualTaskType perpetualTaskType;
  PerpetualTaskClientContext clientContext;
  long intervalSeconds;
  long timeoutMillis;
  String delegateId;
  long lastHeartbeat;
  long createdAt;
  long lastUpdatedAt;
}
