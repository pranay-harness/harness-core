package io.harness.perpetualtask.internal;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskClientContext.PerpetualTaskClientContextKeys;
import io.harness.persistence.AccountAccess;
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
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "PerpetualTaskRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "perpetualTask", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class PerpetualTaskRecord
    implements PersistentEntity, UuidAware, PersistentRegularIterable, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @FdIndex String accountId;
  String perpetualTaskType;
  PerpetualTaskClientContext clientContext;
  long intervalSeconds;
  long timeoutMillis;
  @FdIndex String delegateId;
  String state;
  long lastHeartbeat;

  @FdIndex Long assignerIteration;
  @FdIndex Long resetterIteration;

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

  @UtilityClass
  public static final class PerpetualTaskRecordKeys {
    public static final String client_context_last_updated =
        clientContext + "." + PerpetualTaskClientContextKeys.lastContextUpdated;
    public static final String client_params = clientContext + "." + PerpetualTaskClientContextKeys.clientParams;
  }
}
