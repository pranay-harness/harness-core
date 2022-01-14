package io.harness.ccm.config;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.List;
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
@FieldNameConstants(innerTypeName = "GcpServiceAccountKeys")
@Entity(value = "gcpServiceAccount", noClassnameStored = true)
@OwnedBy(CE)
@StoreIn("events")
public class GcpServiceAccount implements PersistentEntity, UuidAware, AccountAccess, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("no_dup")
                 .unique(true)
                 .field(GcpServiceAccountKeys.accountId)
                 .field(GcpServiceAccountKeys.serviceAccountId)
                 .build())
        .build();
  }
  @Id String uuid;
  @NotEmpty String serviceAccountId;
  @NotEmpty String accountId;
  @NotEmpty String gcpUniqueId;
  @NotEmpty String email;
  long createdAt;
  long lastUpdatedAt;
}
