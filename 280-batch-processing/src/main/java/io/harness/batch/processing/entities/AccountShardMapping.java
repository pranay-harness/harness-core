package io.harness.batch.processing.entities;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "accountShardMapping", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "AccountShardMappingKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
public final class AccountShardMapping
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  int shardId;
  long createdAt;
  long lastUpdatedAt;
}
