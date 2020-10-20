package io.harness.ccm.commons.entities;

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
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
@Entity(value = "latestClusterInfo", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "LatestClusterInfoKeys")
public final class LatestClusterInfo
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  @NotEmpty String accountId;
  @NotEmpty String identifier;
  long createdAt;
  long lastUpdatedAt;
}
