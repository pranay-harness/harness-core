package io.harness.ccm.commons.entities.events;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
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
@StoreIn(DbAliases.CENG)
@Entity(value = "ceExceptionRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CeExceptionRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CE)
public final class CeExceptionRecord implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("accountId_clusterId_createdAt")
                 .field(CeExceptionRecordKeys.accountId)
                 .field(CeExceptionRecordKeys.clusterId)
                 .ascSortField(CeExceptionRecordKeys.createdAt)
                 .build())
        .build();
  }

  @Id String uuid;
  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  String message;
  long createdAt;
}
