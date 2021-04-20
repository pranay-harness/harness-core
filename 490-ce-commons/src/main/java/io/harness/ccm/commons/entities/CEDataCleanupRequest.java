package io.harness.ccm.commons.entities;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceDataCleanupRequest", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CEDataCleanupRequestKeys")
@StoreIn("events")
public class CEDataCleanupRequest
    implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAccess, UpdatedAtAware {
  @Id String uuid;
  String accountId;
  String batchJobType;
  boolean processedRequest;
  int recordCount;
  Instant startAt;
  long createdAt;
  long lastUpdatedAt;
}
