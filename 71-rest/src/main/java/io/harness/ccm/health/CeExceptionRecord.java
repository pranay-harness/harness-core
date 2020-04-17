package io.harness.ccm.health;

import io.harness.annotation.StoreIn;
import io.harness.ccm.health.CeExceptionRecord.CeExceptionRecordKeys;
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
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

@Data
@Builder
@StoreIn("events")
@Entity(value = "ceExceptionRecord", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "CeExceptionRecordKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Indexes(@Index(options = @IndexOptions(name = "accountId_clusterId_createdAt", background = true),
    fields =
    {
      @Field(value = CeExceptionRecordKeys.accountId)
      , @Field(value = CeExceptionRecordKeys.clusterId),
          @Field(value = CeExceptionRecordKeys.createdAt, type = IndexType.ASC)
    }))
public class CeExceptionRecord implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess {
  @Id String uuid;
  @NotEmpty String accountId;
  @NotEmpty String clusterId;
  String message;
  long createdAt;
}
