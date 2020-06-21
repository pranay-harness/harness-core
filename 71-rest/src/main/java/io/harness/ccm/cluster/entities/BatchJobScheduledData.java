package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;

@Data
@Entity(value = "batchJobScheduledData", noClassnameStored = true)

@CdIndex(name = "accountId_batchJobType_endAt",
    fields =
    {
      @Field(BatchJobScheduledDataKeys.accountId)
      , @Field(BatchJobScheduledDataKeys.batchJobType),
          @Field(value = BatchJobScheduledDataKeys.endAt, type = IndexType.DESC)
    })
@FieldNameConstants(innerTypeName = "BatchJobScheduledDataKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
public class BatchJobScheduledData
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String batchJobType;
  long jobRunTimeMillis;
  Instant startAt;
  Instant endAt;
  long createdAt;
  long lastUpdatedAt;

  public BatchJobScheduledData(
      String accountId, String batchJobType, long jobRunTimeMillis, Instant startAt, Instant endAt) {
    this.accountId = accountId;
    this.batchJobType = batchJobType;
    this.jobRunTimeMillis = jobRunTimeMillis;
    this.startAt = startAt;
    this.endAt = endAt;
  }
}
