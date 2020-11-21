package io.harness.ccm.health;

import io.harness.annotation.StoreIn;
import io.harness.ccm.health.CeExceptionRecord.CeExceptionRecordKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
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
@StoreIn("events")
@Entity(value = "ceExceptionRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CeExceptionRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@CdIndex(name = "accountId_clusterId_createdAt",
    fields =
    {
      @Field(value = CeExceptionRecordKeys.accountId)
      , @Field(value = CeExceptionRecordKeys.clusterId),
          @Field(value = CeExceptionRecordKeys.createdAt, type = IndexType.ASC)
    })
public final class CeExceptionRecord implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id String uuid;
  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  String message;
  long createdAt;
}
