package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.ccm.cluster.entities.BatchJobInterval.BatchJobIntervalKeys;
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
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.time.temporal.ChronoUnit;

@Data
@Entity(value = "batchJobInterval", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_batchJobType", unique = true, background = true), fields = {
    @Field(BatchJobIntervalKeys.accountId), @Field(BatchJobIntervalKeys.batchJobType)
  })
})
@FieldNameConstants(innerTypeName = "BatchJobIntervalKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("events")
public class BatchJobInterval implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  String batchJobType;
  ChronoUnit intervalUnit;
  long interval;
  long createdAt;
  long lastUpdatedAt;

  public BatchJobInterval(String accountId, String batchJobType, ChronoUnit intervalUnit, long interval) {
    this.accountId = accountId;
    this.batchJobType = batchJobType;
    this.intervalUnit = intervalUnit;
    this.interval = interval;
  }
}
